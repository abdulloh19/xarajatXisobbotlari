package com.hisobchi.bot.todo.service;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.common.exception.UnauthorizedAccessException;
import com.hisobchi.bot.todo.dto.ParsedTodo;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.*;
import com.hisobchi.bot.todo.repository.TodoProjectRepository;
import com.hisobchi.bot.todo.repository.TodoSubtaskRepository;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.service.TransactionService;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoTaskRepository taskRepository;
    private final TodoSubtaskRepository subtaskRepository;
    private final TodoProjectRepository projectRepository;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    @Transactional
    public TodoTask createTaskFromParsed(User user, ParsedTodo parsed) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");

        Category category = null;
        if (parsed.categoryName() != null && !parsed.categoryName().isBlank()) {
            category = categoryService.findByName(user.getId(), parsed.categoryName(), TransactionType.EXPENSE).orElse(null);
            if (category == null) {
                category = categoryService.getOrCreateCategory(user, parsed.categoryName(), "📌", TransactionType.EXPENSE);
            }
        }

        TodoProject project = null;
        if (parsed.projectName() != null && !parsed.projectName().isBlank()) {
            project = projectRepository.findByUserIdAndNameIgnoreCase(user.getId(), parsed.projectName())
                    .orElseGet(() -> projectRepository.save(TodoProject.builder()
                            .user(user)
                            .name(parsed.projectName())
                            .build()));
        }

        Instant reminderAt = calculateInitialReminderInstant(parsed.dueDate(), parsed.dueTime(), parsed.hasSpecificTime(), parsed.reminderMinutesBefore(), zoneId);

        TodoTask task = TodoTask.builder()
                .user(user)
                .project(project)
                .title(parsed.title())
                .description(parsed.description())
                .status(TodoStatus.OPEN)
                .priority(parsed.priority() != null ? parsed.priority() : TodoPriority.MEDIUM)
                .dueDate(parsed.dueDate())
                .dueTime(parsed.dueTime())
                .hasSpecificTime(parsed.hasSpecificTime())
                .reminderAt(reminderAt)
                .recurrenceType(parsed.recurrenceType() != null ? parsed.recurrenceType() : TodoRecurrenceType.NONE)
                .recurrenceDaysOfWeek(parsed.recurrenceDaysOfWeek())
                .recurrenceIntervalDays(parsed.recurrenceIntervalDays())
                .recurrenceDayOfMonth(parsed.recurrenceDayOfMonth())
                .plannedAmount(parsed.plannedAmount())
                .currency(parsed.currency() != null ? parsed.currency() : "UZS")
                .category(category)
                .build();

        return taskRepository.save(task);
    }

    @Transactional
    public TodoTask markCompleted(Long taskId, Long userId) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        task.setStatus(TodoStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setReminderAt(null); // Stop further reminders for completed task

        TodoTask saved = taskRepository.save(task);

        // Check and generate next occurrence if task is recurring
        if (task.isRecurring()) {
            createNextOccurrence(task);
        }

        return saved;
    }

    @Transactional
    public TodoTask reopenTask(Long taskId, Long userId) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        task.setStatus(TodoStatus.OPEN);
        task.setCompletedAt(null);

        // Recalculate reminder if due date is today or in future
        ZoneId zoneId = ZoneId.of(task.getUser().getTimezone() != null ? task.getUser().getTimezone() : "Asia/Tashkent");
        LocalDate today = LocalDate.now(zoneId);
        if (task.getDueDate() != null && !task.getDueDate().isBefore(today)) {
            task.setReminderAt(calculateInitialReminderInstant(task.getDueDate(), task.getDueTime(), Boolean.TRUE.equals(task.getHasSpecificTime()), 0, zoneId));
        }

        return taskRepository.save(task);
    }

    @Transactional
    public TodoTask cancelTask(Long taskId, Long userId) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        task.setStatus(TodoStatus.CANCELLED);
        task.setReminderAt(null);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        subtaskRepository.deleteByTaskId(taskId);
        taskRepository.delete(task);
    }

    @Transactional
    public TodoTask snoozeTask(Long taskId, Long userId, int snoozeMinutes) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        ZoneId zoneId = ZoneId.of(task.getUser().getTimezone() != null ? task.getUser().getTimezone() : "Asia/Tashkent");
        Instant newReminder = Instant.now().plus(Duration.ofMinutes(snoozeMinutes));

        task.setReminderAt(newReminder);
        task.setSnoozeCount(task.getSnoozeCount() + 1);
        return taskRepository.save(task);
    }

    @Transactional
    public TodoTask snoozeToTomorrowMorning(Long taskId, Long userId) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        ZoneId zoneId = ZoneId.of(task.getUser().getTimezone() != null ? task.getUser().getTimezone() : "Asia/Tashkent");
        LocalDate tomorrow = LocalDate.now(zoneId).plusDays(1);
        ZonedDateTime zdt = tomorrow.atTime(9, 0).atZone(zoneId);

        task.setReminderAt(zdt.toInstant());
        task.setSnoozeCount(task.getSnoozeCount() + 1);
        return taskRepository.save(task);
    }

    @Transactional
    public Transaction recordTaskExpense(Long taskId, Long userId, BigDecimal actualAmount, Long customCategoryId) {
        TodoTask task = getTaskOrThrow(taskId, userId);

        // Idempotency: Prevent duplicate expense creation from a single task
        if (task.getLinkedTransaction() != null) {
            log.warn("Task {} already has linked transaction {}", taskId, task.getLinkedTransaction().getId());
            return task.getLinkedTransaction();
        }

        BigDecimal amount = (actualAmount != null && actualAmount.compareTo(BigDecimal.ZERO) > 0)
                ? actualAmount
                : (task.getPlannedAmount() != null ? task.getPlannedAmount() : BigDecimal.ZERO);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Xarajat summasi 0 dan katta bo‘lishi kerak.");
        }

        Category category = task.getCategory();
        if (customCategoryId != null) {
            try {
                category = categoryService.getById(customCategoryId, userId);
            } catch (Exception ignored) {}
        }
        if (category == null) {
            category = categoryService.getOrCreateCategory(task.getUser(), "Boshqa", "📌", TransactionType.EXPENSE);
        }

        ZoneId zoneId = ZoneId.of(task.getUser().getTimezone() != null ? task.getUser().getTimezone() : "Asia/Tashkent");
        LocalDate txDate = LocalDate.now(zoneId);

        String description = "Vazifa: " + task.getTitle();
        TransactionDto txDto = transactionService.createTransaction(
                task.getUser(),
                TransactionType.EXPENSE,
                amount,
                category,
                description,
                TransactionSource.MANUAL,
                txDate
        );

        Transaction transaction = transactionService.getByIdAndUser(txDto.id(), userId);
        task.setLinkedTransaction(transaction);
        task.setActualAmount(amount);
        taskRepository.save(task);

        return transaction;
    }

    @Transactional
    public void undoTaskExpense(Long taskId, Long userId) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        if (task.getLinkedTransaction() != null) {
            Long txId = task.getLinkedTransaction().getId();
            task.setLinkedTransaction(null);
            task.setActualAmount(null);
            taskRepository.save(task);
            transactionService.deleteTransaction(txId, userId);
        }
    }

    @Transactional(readOnly = true)
    public List<TodoTaskDto> getTodayTasks(Long userId, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        LocalTime now = LocalTime.now(zoneId);
        List<TodoTask> tasks = taskRepository.findByUserIdAndDueDateAndStatusInOrderByDueTimeAsc(
                userId, today, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));
        return tasks.stream().map(t -> toDto(t, today, now)).toList();
    }

    @Transactional(readOnly = true)
    public List<TodoTaskDto> getNext7DaysTasks(Long userId, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        LocalDate end = today.plusDays(7);
        LocalTime now = LocalTime.now(zoneId);
        List<TodoTask> tasks = taskRepository.findByUserIdAndDueDateBetweenAndStatusInOrderByDueDateAscDueTimeAsc(
                userId, today, end, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));
        return tasks.stream().map(t -> toDto(t, today, now)).toList();
    }

    @Transactional(readOnly = true)
    public List<TodoTaskDto> getOverdueTasks(Long userId, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        LocalTime now = LocalTime.now(zoneId);
        List<TodoTask> tasks = taskRepository.findByUserIdAndDueDateLessThanAndStatusInOrderByDueDateAscDueTimeAsc(
                userId, today, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));
        return tasks.stream().map(t -> toDto(t, today, now)).toList();
    }

    @Transactional(readOnly = true)
    public List<TodoTaskDto> getNoDateTasks(Long userId) {
        List<TodoTask> tasks = taskRepository.findByUserIdAndDueDateIsNullAndStatusInOrderByCreatedAtDesc(
                userId, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));
        return tasks.stream().map(t -> toDto(t, LocalDate.now(), LocalTime.now())).toList();
    }

    @Transactional(readOnly = true)
    public List<TodoTaskDto> getCompletedTasks(Long userId, int page, int size) {
        List<TodoTask> tasks = taskRepository.findByUserIdAndStatusOrderByDueDateAscDueTimeAscCreatedAtDesc(
                userId, TodoStatus.COMPLETED, PageRequest.of(page, size));
        return tasks.stream().map(t -> toDto(t, LocalDate.now(), LocalTime.now())).toList();
    }

    @Transactional(readOnly = true)
    public List<TodoTaskDto> searchTasks(Long userId, String query) {
        List<TodoTask> tasks = taskRepository.searchByUserIdAndTitle(userId, query, PageRequest.of(0, 20));
        return tasks.stream().map(t -> toDto(t, LocalDate.now(), LocalTime.now())).toList();
    }

    @Transactional(readOnly = true)
    public TodoTaskDto getTaskDto(Long taskId, Long userId) {
        TodoTask task = getTaskOrThrow(taskId, userId);
        ZoneId zoneId = ZoneId.of(task.getUser().getTimezone() != null ? task.getUser().getTimezone() : "Asia/Tashkent");
        return toDto(task, LocalDate.now(zoneId), LocalTime.now(zoneId));
    }

    @Transactional(readOnly = true)
    public TodoTask getTaskOrThrow(Long taskId, Long userId) {
        TodoTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Vazifa topilmadi: " + taskId));
        if (!task.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Ushbu vazifaga kirish huquqingiz yo‘q.");
        }
        return task;
    }

    private void createNextOccurrence(TodoTask current) {
        if (current.getDueDate() == null) return;

        LocalDate nextDate = switch (current.getRecurrenceType()) {
            case DAILY -> current.getDueDate().plusDays(1);
            case WEEKLY -> current.getDueDate().plusWeeks(1);
            case MONTHLY -> {
                int day = current.getRecurrenceDayOfMonth() != null ? current.getRecurrenceDayOfMonth() : current.getDueDate().getDayOfMonth();
                LocalDate nextMonth = current.getDueDate().plusMonths(1);
                // Safe handling for 29, 30, 31 in short months
                int maxDayInMonth = nextMonth.lengthOfMonth();
                yield nextMonth.withDayOfMonth(Math.min(day, maxDayInMonth));
            }
            case EVERY_N_DAYS -> {
                int n = current.getRecurrenceIntervalDays() != null && current.getRecurrenceIntervalDays() > 0 ? current.getRecurrenceIntervalDays() : 1;
                yield current.getDueDate().plusDays(n);
            }
            case NONE -> null;
        };

        if (nextDate == null) return;
        if (current.getRecurrenceEnd() != null && nextDate.isAfter(current.getRecurrenceEnd())) {
            return; // Recurrence period ended
        }

        ZoneId zoneId = ZoneId.of(current.getUser().getTimezone() != null ? current.getUser().getTimezone() : "Asia/Tashkent");
        Instant nextReminder = calculateInitialReminderInstant(nextDate, current.getDueTime(), Boolean.TRUE.equals(current.getHasSpecificTime()), 0, zoneId);

        TodoTask nextOccurrence = TodoTask.builder()
                .user(current.getUser())
                .project(current.getProject())
                .title(current.getTitle())
                .description(current.getDescription())
                .status(TodoStatus.OPEN)
                .priority(current.getPriority())
                .dueDate(nextDate)
                .dueTime(current.getDueTime())
                .hasSpecificTime(current.getHasSpecificTime())
                .reminderAt(nextReminder)
                .recurrenceType(current.getRecurrenceType())
                .recurrenceDaysOfWeek(current.getRecurrenceDaysOfWeek())
                .recurrenceIntervalDays(current.getRecurrenceIntervalDays())
                .recurrenceDayOfMonth(current.getRecurrenceDayOfMonth())
                .recurrenceEnd(current.getRecurrenceEnd())
                .recurrenceParentId(current.getRecurrenceParentId() != null ? current.getRecurrenceParentId() : current.getId())
                .plannedAmount(current.getPlannedAmount())
                .currency(current.getCurrency())
                .category(current.getCategory())
                .build();

        taskRepository.save(nextOccurrence);
        log.info("Created next recurrence task for parent {} on date {}", current.getId(), nextDate);
    }

    private Instant calculateInitialReminderInstant(LocalDate dueDate, LocalTime dueTime, boolean hasSpecificTime, Integer minutesBefore, ZoneId zoneId) {
        if (dueDate == null) return null;

        LocalTime time = (hasSpecificTime && dueTime != null) ? dueTime : LocalTime.of(9, 0);
        ZonedDateTime zdt = dueDate.atTime(time).atZone(zoneId);

        if (minutesBefore != null && minutesBefore > 0) {
            zdt = zdt.minusMinutes(minutesBefore);
        }

        Instant instant = zdt.toInstant();
        // If calculated reminder is in the past, return null so it doesn't trigger immediately
        return instant.isAfter(Instant.now()) ? instant : null;
    }

    public TodoTaskDto toDto(TodoTask task, LocalDate today, LocalTime currentTime) {
        int totalSub = (int) subtaskRepository.countByTaskId(task.getId());
        int completedSub = (int) subtaskRepository.countByTaskIdAndCompletedTrue(task.getId());

        return TodoTaskDto.builder()
                .id(task.getId())
                .userId(task.getUser().getId())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .dueTime(task.getDueTime())
                .hasSpecificTime(Boolean.TRUE.equals(task.getHasSpecificTime()))
                .reminderAt(task.getReminderAt())
                .recurrenceType(task.getRecurrenceType())
                .plannedAmount(task.getPlannedAmount())
                .actualAmount(task.getActualAmount())
                .currency(task.getCurrency())
                .categoryId(task.getCategory() != null ? task.getCategory().getId() : null)
                .categoryName(task.getCategory() != null ? task.getCategory().getDisplayName() : null)
                .linkedTransactionId(task.getLinkedTransaction() != null ? task.getLinkedTransaction().getId() : null)
                .linkedDebtId(task.getLinkedDebt() != null ? task.getLinkedDebt().getId() : null)
                .totalSubtasks(totalSub)
                .completedSubtasks(completedSub)
                .isOverdue(task.isOverdue(today, currentTime))
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
