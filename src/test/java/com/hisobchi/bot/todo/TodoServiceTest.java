package com.hisobchi.bot.todo;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.todo.dto.ParsedTodo;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.TodoPriority;
import com.hisobchi.bot.todo.entity.TodoRecurrenceType;
import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.repository.TodoProjectRepository;
import com.hisobchi.bot.todo.repository.TodoSubtaskRepository;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.todo.service.TodoService;
import com.hisobchi.bot.transaction.service.TransactionService;
import com.hisobchi.bot.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoTaskRepository taskRepository;
    @Mock
    private TodoSubtaskRepository subtaskRepository;
    @Mock
    private TodoProjectRepository projectRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TodoService todoService;

    private User testUser;
    private final ZoneId zoneId = ZoneId.of("Asia/Tashkent");

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .telegramId(123456L)
                .timezone("Asia/Tashkent")
                .currency("UZS")
                .build();
    }

    @Test
    @DisplayName("Should create task from parsed DTO and persist")
    void testCreateTaskFromParsed() {
        ParsedTodo parsed = ParsedTodo.builder()
                .title("Internet to‘lash")
                .dueDate(LocalDate.now(zoneId).plusDays(1))
                .dueTime(LocalTime.of(18, 0))
                .hasSpecificTime(true)
                .plannedAmount(new BigDecimal("150000"))
                .priority(TodoPriority.HIGH)
                .recurrenceType(TodoRecurrenceType.NONE)
                .build();

        when(taskRepository.save(any(TodoTask.class))).thenAnswer(i -> {
            TodoTask t = i.getArgument(0);
            t.setId(10L);
            return t;
        });

        TodoTask created = todoService.createTaskFromParsed(testUser, parsed);

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("Internet to‘lash", created.getTitle());
        assertEquals(TodoStatus.OPEN, created.getStatus());
        assertEquals(TodoPriority.HIGH, created.getPriority());
        assertEquals(new BigDecimal("150000"), created.getPlannedAmount());
        assertNotNull(created.getDueDate());
    }

    @Test
    @DisplayName("Snooze should NOT change dueDate or dueTime, only reminderAt")
    void testSnoozeDoesNotChangeDueDate() {
        LocalDate originalDueDate = LocalDate.now(zoneId).plusDays(1);
        LocalTime originalDueTime = LocalTime.of(18, 0);

        TodoTask task = TodoTask.builder()
                .id(10L)
                .user(testUser)
                .title("Test Task")
                .dueDate(originalDueDate)
                .dueTime(originalDueTime)
                .hasSpecificTime(true)
                .reminderAt(Instant.now().minusSeconds(60))
                .snoozeCount(0)
                .status(TodoStatus.OPEN)
                .build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(TodoTask.class))).thenAnswer(i -> i.getArgument(0));

        TodoTask snoozed = todoService.snoozeTask(10L, 1L, 15);

        assertEquals(originalDueDate, snoozed.getDueDate());
        assertEquals(originalDueTime, snoozed.getDueTime());
        assertEquals(1, snoozed.getSnoozeCount());
        assertTrue(snoozed.getReminderAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Marking task completed stops further reminders")
    void testMarkCompletedStopsReminders() {
        TodoTask task = TodoTask.builder()
                .id(10L)
                .user(testUser)
                .title("Buy groceries")
                .dueDate(LocalDate.now(zoneId))
                .reminderAt(Instant.now().plusSeconds(300))
                .status(TodoStatus.OPEN)
                .recurrenceType(TodoRecurrenceType.NONE)
                .build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(TodoTask.class))).thenAnswer(i -> i.getArgument(0));

        TodoTask completed = todoService.markCompleted(10L, 1L);

        assertEquals(TodoStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertNull(completed.getReminderAt());
    }
}
