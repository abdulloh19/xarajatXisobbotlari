package com.hisobchi.bot.todo.handler;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory;
import com.hisobchi.bot.todo.dto.ProjectSummaryDto;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.TodoSubtask;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.service.TodoAiAssistantService;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoCallbackHandler {

    private final TelegramApiClient apiClient;
    private final TodoService todoService;
    private final TodoProjectService projectService;
    private final TodoReportService reportService;
    private final TodoAiAssistantService aiAssistantService;
    private final TodoKeyboardFactory keyboardFactory;
    private final ReplyKeyboardFactory replyKeyboardFactory;
    private final UserService userService;

    // Transient map for user editing tracking
    private final Map<Long, Long> userActiveTaskId = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> userAiSuggestedSubtasks = new ConcurrentHashMap<>();

    public void setActiveTaskId(Long userId, Long taskId) {
        userActiveTaskId.put(userId, taskId);
    }

    public Long getActiveTaskId(Long userId) {
        return userActiveTaskId.get(userId);
    }

    public Long removeActiveTaskId(Long userId) {
        return userActiveTaskId.remove(userId);
    }

    public void handle(User user, Long chatId, Integer messageId, String[] parts) {
        if (parts.length < 2) return;
        String action = parts[1];
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");

        switch (action) {
            case "list" -> {
                String filter = parts.length > 2 ? parts[2] : "today";
                int page = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                renderTaskList(user, chatId, messageId, filter, page, zoneId);
            }
            case "view" -> {
                Long taskId = Long.parseLong(parts[2]);
                TodoTaskDto task = todoService.getTaskDto(taskId, user.getId());
                String msg = TodoMessageBuilder.buildTaskDetail(task);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(task), "HTML");
            }
            case "done" -> {
                Long taskId = Long.parseLong(parts[2]);
                TodoTask task = todoService.markCompleted(taskId, user.getId());
                if (task.getPlannedAmount() != null && task.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0 && task.getLinkedTransaction() == null) {
                    String catName = task.getCategory() != null ? task.getCategory().getDisplayName() : "Umumiy";
                    String prompt = String.format("""
                            ✅ <b>Vazifa bajarildi deb belgilandi!</b>
                            ━━━━━━━━━━━━━━━━━━
                            
                            💰 <b>%s</b> so‘mni <b>%s</b> xarajati sifatida yozaymi?
                            """,
                            MoneyFormatter.format(task.getPlannedAmount()),
                            catName
                    );
                    apiClient.editMessageText(chatId, messageId, prompt, keyboardFactory.getExpenseConversionKeyboard(taskId), "HTML");
                } else {
                    TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                    String msg = "✅ <b>Vazifa muvaffaqiyatli bajarildi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                    apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
                }
            }
            case "exp_yes" -> {
                Long taskId = Long.parseLong(parts[2]);
                try {
                    Transaction tx = todoService.recordTaskExpense(taskId, user.getId(), null, null);
                    String msg = String.format("""
                            ✅ <b>Xarajatga muvaffaqiyatli yozildi!</b>
                            ━━━━━━━━━━━━━━━━━━
                            💰 Summa: <b>%s</b>
                            📂 Kategoriya: <b>%s</b>
                            
                            <i>(Agar adashib yozilgan bo‘lsa, pastdagi tugma orqali bekor qilishingiz mumkin)</i>
                            """,
                            MoneyFormatter.format(tx.getAmount()),
                            tx.getCategory() != null ? tx.getCategory().getDisplayName() : "Umumiy"
                    );
                    apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getExpenseRecordedKeyboard(taskId), "HTML");
                } catch (Exception e) {
                    apiClient.sendMessage(chatId, "⚠️ Xarajat yozishda xatolik: " + e.getMessage(), null, null);
                }
            }
            case "exp_edit" -> {
                Long taskId = Long.parseLong(parts[2]);
                setActiveTaskId(user.getId(), taskId);
                userService.updateState(user.getTelegramId(), UserState.WAITING_TODO_EXPENSE_AMOUNT_EDIT);
                apiClient.sendMessage(chatId, "💰 <b>Haqiqiy sarflangan summani kiriting:</b>\n<i>Masalan: 130000 yoki 130 ming</i>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case "exp_skip" -> {
                Long taskId = Long.parseLong(parts[2]);
                TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                String msg = "✔️ <b>Vazifa yopildi. Xarajat daftariga yozilmadi.</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
            }
            case "exp_undo" -> {
                Long taskId = Long.parseLong(parts[2]);
                todoService.undoTaskExpense(taskId, user.getId());
                TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                String msg = "↩️ <b>Xarajat bekor qilindi va hisobdan olib tashlandi.</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
            }
            case "reopen" -> {
                Long taskId = Long.parseLong(parts[2]);
                todoService.reopenTask(taskId, user.getId());
                TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                String msg = "🔄 <b>Vazifa qayta ochildi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
            }
            case "cancel" -> {
                Long taskId = Long.parseLong(parts[2]);
                todoService.cancelTask(taskId, user.getId());
                TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                String msg = "❌ <b>Vazifa bekor qilindi.</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
            }
            case "delete" -> {
                Long taskId = Long.parseLong(parts[2]);
                todoService.deleteTask(taskId, user.getId());
                apiClient.editMessageText(chatId, messageId, "🗑 <b>Vazifa butkul o‘chirildi.</b>", null, "HTML");
            }
            case "snooze_menu" -> {
                Long taskId = Long.parseLong(parts[2]);
                apiClient.editMessageText(chatId, messageId, "⏰ <b>Eslatmani qancha vaqtga surmoqchisiz?</b>",
                        keyboardFactory.getSnoozeMenuKeyboard(taskId), "HTML");
            }
            case "snooze" -> {
                Long taskId = Long.parseLong(parts[2]);
                int mins = Integer.parseInt(parts[3]);
                todoService.snoozeTask(taskId, user.getId(), mins);
                TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                String msg = "⏰ <b>Eslatma " + mins + " daqiqaga surildi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
            }
            case "snooze_tmr" -> {
                Long taskId = Long.parseLong(parts[2]);
                todoService.snoozeToTomorrowMorning(taskId, user.getId());
                TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                String msg = "📅 <b>Eslatma ertaga soat 09:00 ga surildi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
            }
            case "edit_menu" -> {
                Long taskId = Long.parseLong(parts[2]);
                apiClient.editMessageText(chatId, messageId, "✏️ <b>Nimani o‘zgartirmoqchisiz?</b>",
                        keyboardFactory.getEditMenuKeyboard(taskId), "HTML");
            }
            case "edit_t" -> {
                Long taskId = Long.parseLong(parts[2]);
                setActiveTaskId(user.getId(), taskId);
                userService.updateState(user.getTelegramId(), UserState.WAITING_TODO_EDIT_TITLE);
                apiClient.sendMessage(chatId, "📝 <b>Yangi vazifa nomini yozing:</b>", replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case "edit_d" -> {
                Long taskId = Long.parseLong(parts[2]);
                setActiveTaskId(user.getId(), taskId);
                userService.updateState(user.getTelegramId(), UserState.WAITING_TODO_EDIT_DATE);
                apiClient.sendMessage(chatId, "📅 <b>Yangi muddatni yozing (masalan: <i>ertaga 15:00</i> yoki <i>juma kuni</i>):</b>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case "edit_a" -> {
                Long taskId = Long.parseLong(parts[2]);
                setActiveTaskId(user.getId(), taskId);
                userService.updateState(user.getTelegramId(), UserState.WAITING_TODO_AMOUNT);
                apiClient.sendMessage(chatId, "💰 <b>Yangi rejalashtirilgan summani kiriting:</b>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case "ai_split" -> {
                Long taskId = Long.parseLong(parts[2]);
                TodoTask task = todoService.getTaskOrThrow(taskId, user.getId());
                List<String> suggested = aiAssistantService.suggestSubtasks(task.getTitle());
                userAiSuggestedSubtasks.put(user.getId(), suggested);

                StringBuilder sb = new StringBuilder();
                sb.append("🤖 <b>AI tomonidan tavsiya etilgan qadamlar:</b>\n\n");
                for (int i = 0; i < suggested.size(); i++) {
                    sb.append(i + 1).append(". ").append(suggested.get(i)).append("\n");
                }
                sb.append("\nUshbu qadamlarni vazifaga kichik ishlar sifatida qo‘shasizmi?");
                apiClient.editMessageText(chatId, messageId, sb.toString(),
                        keyboardFactory.getAiDecompositionConfirmKeyboard(taskId, suggested.size()), "HTML");
            }
            case "ai_save" -> {
                Long taskId = Long.parseLong(parts[2]);
                List<String> subtasks = userAiSuggestedSubtasks.remove(user.getId());
                if (subtasks != null) {
                    for (String title : subtasks) {
                        projectService.addSubtask(taskId, user.getId(), title);
                    }
                }
                TodoTaskDto dto = todoService.getTaskDto(taskId, user.getId());
                String msg = "✅ <b>Kichik qadamlar saqlandi!</b>\n\n" + TodoMessageBuilder.buildTaskDetail(dto);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskCardKeyboard(dto), "HTML");
            }
            case "subtasks" -> {
                Long taskId = Long.parseLong(parts[2]);
                List<TodoSubtask> list = projectService.getSubtasks(taskId, user.getId());
                apiClient.editMessageText(chatId, messageId, "📋 <b>Kichik ishlar ro‘yxati:</b>\n(Bajarilganini belgilash uchun ustiga bosing)",
                        keyboardFactory.getSubtasksKeyboard(taskId, list), "HTML");
            }
            case "st_toggle" -> {
                Long subtaskId = Long.parseLong(parts[2]);
                TodoSubtask st = projectService.toggleSubtask(subtaskId, user.getId());
                List<TodoSubtask> list = projectService.getSubtasks(st.getTask().getId(), user.getId());
                apiClient.editMessageText(chatId, messageId, "📋 <b>Kichik ishlar ro‘yxati:</b>\n(Bajarilganini belgilash uchun ustiga bosing)",
                        keyboardFactory.getSubtasksKeyboard(st.getTask().getId(), list), "HTML");
            }
            case "st_del" -> {
                Long subtaskId = Long.parseLong(parts[2]);
                TodoSubtask st = projectService.toggleSubtask(subtaskId, user.getId()); // verify auth
                Long taskId = st.getTask().getId();
                projectService.deleteSubtask(subtaskId, user.getId());
                List<TodoSubtask> list = projectService.getSubtasks(taskId, user.getId());
                apiClient.editMessageText(chatId, messageId, "📋 <b>Kichik ishlar ro‘yxati:</b>\n(Bajarilganini belgilash uchun ustiga bosing)",
                        keyboardFactory.getSubtasksKeyboard(taskId, list), "HTML");
            }
            case "st_add" -> {
                Long taskId = Long.parseLong(parts[2]);
                setActiveTaskId(user.getId(), taskId);
                userService.updateState(user.getTelegramId(), UserState.WAITING_TODO_SUBTASK_TITLE);
                apiClient.sendMessage(chatId, "📋 <b>Qo‘shmoqchi bo‘lgan kichik ish nomini yozing:</b>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case "proj" -> {
                List<ProjectSummaryDto> projs = projectService.getUserProjects(user.getId());
                String msg = TodoMessageBuilder.buildProjectListMessage(projs);
                apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getProjectListKeyboard(), "HTML");
            }
            case "proj_add" -> {
                userService.updateState(user.getTelegramId(), UserState.WAITING_TODO_PROJECT_NAME);
                apiClient.sendMessage(chatId, "📁 <b>Yangi loyiha nomini kiriting (masalan: <i>Uy ta'miri</i>):</b>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case "report" -> {
                if ("planned".equals(parts[2])) {
                    String msg = reportService.formatPlannedPaymentsReport(user);
                    apiClient.editMessageText(chatId, messageId, msg, keyboardFactory.getTaskListKeyboard("today", 0, 1, List.of()), "HTML");
                }
            }
            case "add" -> {
                userService.updateState(user.getTelegramId(), UserState.WAITING_TODO_TITLE);
                apiClient.sendMessage(chatId, "✍️ <b>Vazifani tabiiy tilda yozing:</b>\n\nMasalan:\n<i>\"Ertaga 18:00 da internetga 150 ming to‘lash\"</i>\n<i>\"Juma kuni hisobot topshirish\"</i>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            default -> log.warn("Unknown todo callback: {}", action);
        }
    }

    private void renderTaskList(User user, Long chatId, Integer messageId, String filter, int page, ZoneId zoneId) {
        int pageSize = 5;
        List<TodoTaskDto> allTasks = switch (filter) {
            case "week" -> todoService.getNext7DaysTasks(user.getId(), zoneId);
            case "overdue" -> todoService.getOverdueTasks(user.getId(), zoneId);
            case "nodate" -> todoService.getNoDateTasks(user.getId());
            case "done" -> todoService.getCompletedTasks(user.getId(), 0, 100);
            default -> todoService.getTodayTasks(user.getId(), zoneId);
        };

        int totalTasks = allTasks.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalTasks / pageSize));
        int safePage = Math.min(page, totalPages - 1);
        int fromIndex = safePage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalTasks);

        List<TodoTaskDto> pageTasks = (fromIndex < totalTasks) ? allTasks.subList(fromIndex, toIndex) : List.of();

        String filterName = switch (filter) {
            case "week" -> "Yaqin 7 kun";
            case "overdue" -> "Kechikkan ishlar";
            case "nodate" -> "Sanasiz vazifalar";
            case "done" -> "Bajarilgan ishlar";
            default -> "Bugungi kun";
        };

        String msg = TodoMessageBuilder.buildTaskListMessage(filterName, pageTasks, safePage, totalPages);
        apiClient.editMessageText(chatId, messageId, msg,
                keyboardFactory.getTaskListKeyboard(filter, safePage, totalPages, pageTasks), "HTML");
    }
}
