package com.hisobchi.bot.todo.service;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.service.NotificationSettingsService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardButton;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardMarkup;
import com.hisobchi.bot.todo.entity.TodoReminderLog;
import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.repository.TodoReminderLogRepository;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoReminderService {

    private final TodoTaskRepository taskRepository;
    private final TodoReminderLogRepository reminderLogRepository;
    private final NotificationSettingsService settingsService;
    private final TelegramApiClient apiClient;

    @Transactional
    public void processDueReminders() {
        Instant now = Instant.now();
        List<TodoTask> dueTasks = taskRepository.findDueReminders(now);

        if (dueTasks.isEmpty()) {
            return;
        }

        // Group due tasks by user to handle quiet hours and aggregation
        Map<User, List<TodoTask>> userTasksMap = dueTasks.stream()
                .collect(Collectors.groupingBy(TodoTask::getUser));

        for (Map.Entry<User, List<TodoTask>> entry : userTasksMap.entrySet()) {
            User user = entry.getKey();
            List<TodoTask> tasks = entry.getValue();

            try {
                processUserDueTasks(user, tasks);
            } catch (Exception e) {
                log.error("Failed to process todo reminders for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
    }

    private void processUserDueTasks(User user, List<TodoTask> tasks) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
        ZonedDateTime userNow = ZonedDateTime.now(zoneId);
        LocalTime userTime = userNow.toLocalTime();

        NotificationSettings settings = settingsService.getOrCreateSettings(user);

        // 1. Quiet Hours check
        if (Boolean.TRUE.equals(settings.getQuietHoursEnabled())) {
            LocalTime start = parseTime(settings.getQuietHoursStart(), LocalTime.of(23, 0));
            LocalTime end = parseTime(settings.getQuietHoursEnd(), LocalTime.of(7, 0));

            if (isInQuietHours(userTime, start, end)) {
                // Postpone reminders until quiet hours end
                LocalDate targetDate = (userTime.isAfter(start) || userTime.equals(start)) ? userNow.toLocalDate().plusDays(1) : userNow.toLocalDate();
                Instant resumeInstant = targetDate.atTime(end).atZone(zoneId).toInstant();

                for (TodoTask task : tasks) {
                    task.setReminderAt(resumeInstant);
                    taskRepository.save(task);
                }
                log.debug("Postponed {} reminders for user {} due to quiet hours until {}", tasks.size(), user.getId(), resumeInstant);
                return;
            }
        }

        // 2. Downtime Aggregation: If user has > 3 overdue reminders, send aggregated summary
        if (tasks.size() > 3) {
            sendAggregatedReminder(user, tasks);
            for (TodoTask t : tasks) {
                t.setReminderAt(null);
                taskRepository.save(t);
                logDelivery(t, user, "DELIVERED_AGGREGATED", null);
            }
            return;
        }

        // 3. Otherwise send individual reminder cards
        for (TodoTask task : tasks) {
            try {
                sendSingleReminder(user, task);
                task.setReminderAt(null);
                taskRepository.save(task);
                logDelivery(task, user, "DELIVERED", null);
            } catch (Exception e) {
                log.error("Failed sending reminder for task {}: {}", task.getId(), e.getMessage());
                // Exponential backoff: retry in 5 minutes
                task.setReminderAt(Instant.now().plus(Duration.ofMinutes(5)));
                taskRepository.save(task);
                logDelivery(task, user, "FAILED", e.getMessage());
            }
        }
    }

    private void sendSingleReminder(User user, TodoTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ <b>Vazifa vaqti bo‘ldi!</b>\n\n");
        sb.append(task.getPriority().getEmoji()).append(" <b>").append(escapeHtml(task.getTitle())).append("</b>\n");

        if (task.getProject() != null) {
            sb.append("📁 Loyiha: <b>").append(escapeHtml(task.getProject().getName())).append("</b>\n");
        }

        if (task.getPlannedAmount() != null && task.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💰 Rejalashtirilgan to‘lov: <b>").append(MoneyFormatter.format(task.getPlannedAmount())).append("</b>\n");
        }

        if (task.getCategory() != null) {
            sb.append("📂 Kategoriya: <b>").append(escapeHtml(task.getCategory().getDisplayName())).append("</b>\n");
        }

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("✅ Bajarildi").callbackData("todo:done:" + task.getId()).build(),
                                InlineKeyboardButton.builder().text("⏰ +15 daq").callbackData("todo:snooze:" + task.getId() + ":15").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("⏰ +1 soat").callbackData("todo:snooze:" + task.getId() + ":60").build(),
                                InlineKeyboardButton.builder().text("📅 Ertaga").callbackData("todo:snooze_tmr:" + task.getId()).build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("✏️ Tahrirlash").callbackData("todo:view:" + task.getId()).build(),
                                InlineKeyboardButton.builder().text("❌ Bekor qilish").callbackData("todo:cancel:" + task.getId()).build()
                        )
                ))
                .build();

        apiClient.sendMessage(user.getTelegramId(), sb.toString(), keyboard, "HTML");
    }

    private void sendAggregatedReminder(User user, List<TodoTask> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ <b>Diqqat! Sizda ").append(tasks.size()).append(" ta eslatma mavjud:</b>\n\n");

        int count = 0;
        for (TodoTask t : tasks) {
            if (++count > 5) break;
            sb.append("• ").append(t.getPriority().getEmoji()).append(" <b>").append(escapeHtml(t.getTitle())).append("</b>");
            if (t.getPlannedAmount() != null) {
                sb.append(" (").append(MoneyFormatter.format(t.getPlannedAmount())).append(")");
            }
            sb.append("\n");
        }
        if (tasks.size() > 5) {
            sb.append("<i>... va yana ").append(tasks.size() - 5).append(" ta vazifa</i>\n");
        }

        sb.append("\nQuyidagi tugma orqali barchasini ko‘rishingiz mumkin:");

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(InlineKeyboardButton.builder().text("📋 Vazifalar ro‘yxati").callbackData("todo:list:today").build()),
                        List.of(InlineKeyboardButton.builder().text("🏠 Asosiy menyu").callbackData("menu:main").build())
                ))
                .build();

        apiClient.sendMessage(user.getTelegramId(), sb.toString(), keyboard, "HTML");
    }

    private boolean isInQuietHours(LocalTime time, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        } else {
            // E.g. 23:00 to 07:00
            return !time.isBefore(start) || time.isBefore(end);
        }
    }

    private LocalTime parseTime(String timeStr, LocalTime defaultTime) {
        if (timeStr == null || timeStr.isBlank()) return defaultTime;
        try {
            return LocalTime.parse(timeStr.trim());
        } catch (Exception e) {
            return defaultTime;
        }
    }

    private void logDelivery(TodoTask task, User user, String status, String error) {
        try {
            reminderLogRepository.save(TodoReminderLog.builder()
                    .task(task)
                    .user(user)
                    .status(status)
                    .deliveryError(error)
                    .build());
        } catch (Exception e) {
            log.warn("Could not log todo reminder delivery: {}", e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
