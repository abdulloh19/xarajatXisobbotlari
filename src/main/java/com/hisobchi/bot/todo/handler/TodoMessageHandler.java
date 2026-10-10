package com.hisobchi.bot.todo.handler;

import com.hisobchi.bot.ai.service.UzbekAmountParser;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.common.util.UzbekDateParser;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory;
import com.hisobchi.bot.todo.dto.ParsedTodo;
import com.hisobchi.bot.todo.dto.ProjectSummaryDto;
import com.hisobchi.bot.todo.dto.TodoDailyBriefDto;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.TodoProject;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.service.TodoNlpService;
import com.hisobchi.bot.todo.service.TodoProjectService;
import com.hisobchi.bot.todo.service.TodoReportService;
import com.hisobchi.bot.todo.service.TodoService;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserState;
import com.hisobchi.bot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoMessageHandler {

    private final TelegramApiClient apiClient;
    private final TodoService todoService;
    private final TodoProjectService projectService;
    private final TodoReportService reportService;
    private final TodoNlpService nlpService;
    private final TodoKeyboardFactory keyboardFactory;
    private final ReplyKeyboardFactory replyKeyboardFactory;
    private final UserService userService;
    private final UzbekAmountParser amountParser;
    private final UzbekDateParser dateParser;
    private final TodoCallbackHandler callbackHandler;

    public boolean handleState(User user, Long chatId, String text) {
        UserState state = user.getState();
        if (state == null) return false;

        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");

        switch (state) {
            case WAITING_TODO_TITLE -> {
                Optional<ParsedTodo> parsedOpt = nlpService.parseSingle(text, zoneId);
                if (parsedOpt.isPresent()) {
                    TodoTask task = todoService.createTaskFromParsed(user, parsedOpt.get());
                    TodoTaskDto dto = todoService.getTaskDto(task.getId(), user.getId());
                    String msg = TodoMessageBuilder.buildTaskCreatedMessage(dto);
                    apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskCreatedKeyboard(task.getId()), "HTML");
                } else {
                    apiClient.sendMessage(chatId, "⚠️ Vazifani aniqlab bo‘lmadi. Iltimos, aniqroq yozing.",
                            replyKeyboardFactory.getMainMenu(), null);
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
            case WAITING_TODO_EXPENSE_AMOUNT_EDIT -> {
                Long taskId = callbackHandler.removeActiveTaskId(user.getId());
                if (taskId == null) {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    return false;
                }
                Optional<BigDecimal> amtOpt = amountParser.parse(text);
                if (amtOpt.isEmpty() || amtOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>150000</code>):",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                    return true;
                }
                try {
                    Transaction tx = todoService.recordTaskExpense(taskId, user.getId(), amtOpt.get(), null);
                    String msg = String.format("""
                            ✅ <b>O‘zgartirilgan summa bilan xarajatga yozildi!</b>
                            ━━━━━━━━━━━━━━━━━━
                            💰 Summa: <b>%s</b>
                            📂 Kategoriya: <b>%s</b>
                            """,
                            MoneyFormatter.format(tx.getAmount()),
                            tx.getCategory() != null ? tx.getCategory().getDisplayName() : "Umumiy"
                    );
                    apiClient.sendMessage(chatId, msg, keyboardFactory.getExpenseRecordedKeyboard(taskId), "HTML");
                } catch (Exception e) {
                    apiClient.sendMessage(chatId, "⚠️ Xatolik yuz berdi: " + e.getMessage(), replyKeyboardFactory.getMainMenu(), null);
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
            case WAITING_TODO_EDIT_TITLE -> {
                Long taskId = callbackHandler.removeActiveTaskId(user.getId());
                if (taskId != null) {
                    TodoTask task = todoService.getTaskOrThrow(taskId, user.getId());
                    task.setTitle(text.trim());
                    TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                    String msg = "✅ <b>Vazifa nomi yangilandi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                    apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
            case WAITING_TODO_EDIT_DATE -> {
                Long taskId = callbackHandler.removeActiveTaskId(user.getId());
                if (taskId != null) {
                    Optional<ParsedTodo> parsedOpt = nlpService.parseSingle(text, zoneId);
                    TodoTask task = todoService.getTaskOrThrow(taskId, user.getId());
                    if (parsedOpt.isPresent() && parsedOpt.get().dueDate() != null) {
                        task.setDueDate(parsedOpt.get().dueDate());
                        task.setDueTime(parsedOpt.get().dueTime());
                        task.setHasSpecificTime(parsedOpt.get().hasSpecificTime());
                    }
                    TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                    String msg = "✅ <b>Muddat yangilandi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                    apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
            case WAITING_TODO_AMOUNT -> {
                Long taskId = callbackHandler.removeActiveTaskId(user.getId());
                if (taskId != null) {
                    Optional<BigDecimal> amt = amountParser.parse(text);
                    if (amt.isPresent()) {
                        TodoTask task = todoService.getTaskOrThrow(taskId, user.getId());
                        task.setPlannedAmount(amt.get());
                    }
                    TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                    String msg = "✅ <b>Rejalashtirilgan summa yangilandi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                    apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
            case WAITING_TODO_PROJECT_NAME -> {
                TodoProject p = projectService.createProject(user, text, null, "📁");
                apiClient.sendMessage(chatId, "✅ <b>Yangi loyiha yaratildi:</b> " + p.getName(),
                        replyKeyboardFactory.getMainMenu(), "HTML");
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
            case WAITING_TODO_SUBTASK_TITLE -> {
                Long taskId = callbackHandler.removeActiveTaskId(user.getId());
                if (taskId != null) {
                    projectService.addSubtask(taskId, user.getId(), text);
                    TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                    String msg = "✅ <b>Kichik qadam qo‘shildi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                    apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean handleCommandOrButton(User user, Long chatId, String text) {
        String trimmed = text.trim();
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");

        if (trimmed.equalsIgnoreCase("✅ Vazifalar") || trimmed.equalsIgnoreCase("Vazifalar")
                || trimmed.equalsIgnoreCase("/vazifalar") || trimmed.equalsIgnoreCase("/todo") || trimmed.equalsIgnoreCase("/tasks")) {
            sendTaskList(user, chatId, "today", 0, zoneId);
            return true;
        }

        if (trimmed.equalsIgnoreCase("📅 Bugun") || trimmed.equalsIgnoreCase("/bugun")) {
            TodoDailyBriefDto brief = reportService.getDailyBrief(user);
            String msg = reportService.formatDailyBriefMessage(brief);
            apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskListKeyboard("today", 0, 1, brief.todayTasks()), "HTML");
            return true;
        }

        if (trimmed.equalsIgnoreCase("📁 Loyihalar") || trimmed.equalsIgnoreCase("/loyihalar") || trimmed.equalsIgnoreCase("/projects")) {
            List<ProjectSummaryDto> projs = projectService.getUserProjects(user.getId());
            String msg = TodoMessageBuilder.buildProjectListMessage(projs);
            apiClient.sendMessage(chatId, msg, keyboardFactory.getProjectListKeyboard(), "HTML");
            return true;
        }

        // Natural language task creation
        if (nlpService.isTaskMessage(trimmed)) {
            List<ParsedTodo> parsedList = nlpService.parseMultiOrSingle(trimmed, zoneId);
            if (!parsedList.isEmpty()) {
                if (parsedList.size() == 1) {
                    TodoTask task = todoService.createTaskFromParsed(user, parsedList.get(0));
                    TodoTaskDto dto = todoService.getTaskDto(task.getId(), user.getId());
                    String msg = TodoMessageBuilder.buildTaskCreatedMessage(dto);
                    apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskCreatedKeyboard(task.getId()), "HTML");
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("✅ <b>").append(parsedList.size()).append(" ta yangi vazifa saqlandi:</b>\n\n");
                    for (ParsedTodo pt : parsedList) {
                        TodoTask task = todoService.createTaskFromParsed(user, pt);
                        sb.append("• ").append(task.getPriority().getEmoji()).append(" <b>").append(task.getTitle()).append("</b>");
                        if (task.getDueDate() != null) {
                            sb.append(" <i>(").append(DateTimeUtils.formatUzbekDate(task.getDueDate())).append(")</i>");
                        }
                        sb.append("\n");
                    }
                    apiClient.sendMessage(chatId, sb.toString(), keyboardFactory.getTaskListKeyboard("today", 0, 1, List.of()), "HTML");
                }
                return true;
            }
        }

        return false;
    }

    private void sendTaskList(User user, Long chatId, String filter, int page, ZoneId zoneId) {
        int pageSize = 5;
        List<TodoTaskDto> allTasks = todoService.getTodayTasks(user.getId(), zoneId);
        int totalTasks = allTasks.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalTasks / pageSize));
        int safePage = Math.min(page, totalPages - 1);
        int fromIndex = safePage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalTasks);
        List<TodoTaskDto> pageTasks = (fromIndex < totalTasks) ? allTasks.subList(fromIndex, toIndex) : List.of();

        String msg = TodoMessageBuilder.buildTaskListMessage("Bugungi kun", pageTasks, safePage, totalPages);
        apiClient.sendMessage(chatId, msg, keyboardFactory.getTaskListKeyboard("today", safePage, totalPages, pageTasks), "HTML");
    }
}
