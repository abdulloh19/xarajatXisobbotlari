package com.hisobchi.bot.todo.service;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.todo.dto.TodoDailyBriefDto;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.TodoPriority;
import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoReportService {

    private final TodoService todoService;
    private final TodoTaskRepository taskRepository;

    @Transactional(readOnly = true)
    public TodoDailyBriefDto getDailyBrief(User user) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
        List<TodoTaskDto> todayTasks = todoService.getTodayTasks(user.getId(), zoneId);
        List<TodoTaskDto> overdueTasks = todoService.getOverdueTasks(user.getId(), zoneId);

        List<TodoTaskDto> plannedPayments = todayTasks.stream()
                .filter(t -> t.plannedAmount() != null && t.plannedAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal totalPlannedPayment = plannedPayments.stream()
                .map(TodoTaskDto::plannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Recommend top 3 tasks: Overdue first, then High priority, then earliest due time
        List<TodoTaskDto> recommended = todayTasks.stream()
                .sorted(Comparator.comparingInt((TodoTaskDto t) -> t.isOverdue() ? 0 : 1)
                        .thenComparingInt(t -> t.priority() == TodoPriority.HIGH ? 0 : (t.priority() == TodoPriority.MEDIUM ? 1 : 2))
                        .thenComparing(t -> t.dueTime() != null ? t.dueTime() : java.time.LocalTime.MAX))
                .limit(3)
                .toList();

        return TodoDailyBriefDto.builder()
                .todayTasks(todayTasks)
                .overdueTasks(overdueTasks)
                .todayPlannedPayments(plannedPayments)
                .totalPlannedPaymentToday(totalPlannedPayment)
                .topRecommendedTasks(recommended)
                .build();
    }

    public String formatDailyBriefMessage(TodoDailyBriefDto brief) {
        StringBuilder sb = new StringBuilder();
        sb.append("☀️ <b>KUNLIK VAZIFALAR SHARHI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (!brief.overdueTasks().isEmpty()) {
            sb.append("⚠️ <b>Kechikkan vazifalar (").append(brief.overdueTasks().size()).append(" ta):</b>\n");
            for (TodoTaskDto t : brief.overdueTasks()) {
                sb.append("• 🔴 <s>").append(escapeHtml(t.title())).append("</s>");
                if (t.dueDate() != null) {
                    sb.append(" <i>(").append(DateTimeUtils.formatUzbekDate(t.dueDate())).append(")</i>");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("📋 <b>Bugungi vazifalar: ").append(brief.todayTasks().size()).append(" ta</b>\n");
        if (brief.todayTasks().isEmpty()) {
            sb.append("<i>Bugun uchun ochiq vazifalar yo‘q. Yangi maqsadlar belgilang!</i>\n");
        } else {
            for (TodoTaskDto t : brief.todayTasks()) {
                sb.append("• ").append(t.priority().getEmoji()).append(" <b>").append(escapeHtml(t.title())).append("</b>");
                if (t.dueTime() != null) {
                    sb.append(" <i>(").append(t.dueTime()).append(")</i>");
                }
                if (t.plannedAmount() != null) {
                    sb.append(" — <code>").append(MoneyFormatter.format(t.plannedAmount())).append("</code>");
                }
                sb.append("\n");
            }
        }
        sb.append("\n");

        if (brief.totalPlannedPaymentToday().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💰 <b>Bugungi rejalashtirilgan to‘lovlar:</b> <b>")
                    .append(MoneyFormatter.format(brief.totalPlannedPaymentToday())).append("</b>\n\n");
        }

        if (!brief.topRecommendedTasks().isEmpty()) {
            sb.append("🎯 <b>Boshlash uchun tavsiya etilgan 3 ta muhim ish:</b>\n");
            int i = 1;
            for (TodoTaskDto t : brief.topRecommendedTasks()) {
                sb.append(i++).append(". ").append(escapeHtml(t.title())).append("\n");
            }
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String formatWeeklyReviewMessage(User user) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
        LocalDate today = LocalDate.now(zoneId);
        LocalDate nextWeek = today.plusDays(7);

        long openCount = taskRepository.countByUserIdAndStatusIn(user.getId(), List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));
        long completedCount = taskRepository.countByUserIdAndStatus(user.getId(), TodoStatus.COMPLETED);

        BigDecimal plannedNext7Days = taskRepository.sumPlannedAmountByUserIdAndDueDateBetween(
                user.getId(), today, nextWeek, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));

        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>HAFTALIK VAZIFALAR VA TO‘LOVLAR SHARHI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("✅ <b>Bajarilgan vazifalar:</b> ").append(completedCount).append(" ta\n");
        sb.append("📌 <b>Qolgan ochiq vazifalar:</b> ").append(openCount).append(" ta\n\n");

        sb.append("💰 <b>Keyingi 7 kundagi rejalashtirilgan to‘lovlar:</b>\n");
        sb.append("<b>").append(MoneyFormatter.format(plannedNext7Days)).append("</b>\n\n");

        sb.append("<i>Rejalashtirilgan to‘lovlar haqiqiy xarajatlarga faqat siz bajarganingizdan so‘ng qo‘shiladi.</i>");

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String formatPlannedPaymentsReport(User user) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
        LocalDate today = LocalDate.now(zoneId);
        LocalDate in7Days = today.plusDays(7);
        LocalDate in30Days = today.plusDays(30);

        BigDecimal next7 = taskRepository.sumPlannedAmountByUserIdAndDueDateBetween(
                user.getId(), today, in7Days, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));
        BigDecimal next30 = taskRepository.sumPlannedAmountByUserIdAndDueDateBetween(
                user.getId(), today, in30Days, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS));

        StringBuilder sb = new StringBuilder();
        sb.append("📅 <b>REJALASHTIRILGAN TO‘LOVLAR HISOBOTI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("🗓 <b>Yaqin 7 kun ichida:</b> <b>").append(MoneyFormatter.format(next7)).append("</b>\n");
        sb.append("🗓 <b>Yaqin 30 kun ichida:</b> <b>").append(MoneyFormatter.format(next30)).append("</b>\n\n");
        sb.append("💡 <i>Ushbu to‘lovlar oldindan moliyaviy byudjetingizni to‘g‘ri taqsimlashga yordam beradi.</i>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
