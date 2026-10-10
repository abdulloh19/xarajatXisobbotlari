package com.hisobchi.bot.todo.handler;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.todo.dto.ProjectSummaryDto;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.TodoRecurrenceType;
import com.hisobchi.bot.todo.entity.TodoStatus;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TodoMessageBuilder {

    public static String buildTaskDetail(TodoTaskDto task) {
        StringBuilder sb = new StringBuilder();
        sb.append(task.priority().getEmoji()).append(" <b>").append(escapeHtml(task.title())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        // Status
        String statusText = switch (task.status()) {
            case OPEN -> "⏳ Ochiq";
            case IN_PROGRESS -> "⚙️ Jarayonda";
            case COMPLETED -> "✅ Bajarilgan";
            case CANCELLED -> "❌ Bekor qilingan";
        };
        sb.append("📌 <b>Holat:</b> ").append(statusText);
        if (task.isOverdue()) {
            sb.append(" <i>(⚠️ Kechikkan!)</i>");
        }
        sb.append("\n");

        // Muddat
        if (task.dueDate() != null) {
            sb.append("📅 <b>Muddat:</b> ").append(DateTimeUtils.formatUzbekDate(task.dueDate()));
            if (task.hasSpecificTime() && task.dueTime() != null) {
                sb.append(", ").append(task.dueTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            sb.append("\n");
        } else {
            sb.append("📅 <b>Muddat:</b> <i>Belgilanmagan (sanasiz)</i>\n");
        }

        // Recurrence
        if (task.recurrenceType() != null && task.recurrenceType() != TodoRecurrenceType.NONE) {
            sb.append("🔄 <b>Takrorlanish:</b> ").append(task.recurrenceType().getDisplayName()).append("\n");
        }

        // Project
        if (task.projectName() != null) {
            sb.append("📁 <b>Loyiha:</b> ").append(escapeHtml(task.projectName())).append("\n");
        }

        // Planned amount & Expense
        if (task.plannedAmount() != null && task.plannedAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💰 <b>Reja summa:</b> <code>").append(MoneyFormatter.format(task.plannedAmount())).append("</code>\n");
            if (task.categoryName() != null) {
                sb.append("📂 <b>Kategoriya:</b> ").append(escapeHtml(task.categoryName())).append("\n");
            }
            if (task.linkedTransactionId() != null) {
                sb.append("💳 <b>Xarajatga yozilgan:</b> ✅ (Operatsiya #").append(task.linkedTransactionId()).append(")\n");
            }
        }

        // Description
        if (task.description() != null && !task.description().isBlank()) {
            sb.append("\n📝 <i>").append(escapeHtml(task.description())).append("</i>\n");
        }

        // Subtasks progress bar
        if (task.totalSubtasks() > 0) {
            sb.append("\n📋 <b>Kichik qadamlar:</b> ")
                    .append(task.completedSubtasks()).append("/").append(task.totalSubtasks())
                    .append(" (").append(task.getSubtaskProgressPercent()).append("%)\n")
                    .append(renderProgressBar(task.getSubtaskProgressPercent())).append("\n");
        }

        return sb.toString();
    }

    public static String buildTaskCreatedMessage(TodoTaskDto task) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ <b>Yangi vazifa saqlandi!</b>\n\n");
        sb.append(task.priority().getEmoji()).append(" <b>").append(escapeHtml(task.title())).append("</b>\n");

        if (task.dueDate() != null) {
            sb.append("📅 ").append(DateTimeUtils.formatUzbekDate(task.dueDate()));
            if (task.hasSpecificTime() && task.dueTime() != null) {
                sb.append(", ").append(task.dueTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            sb.append("\n");
        }

        if (task.plannedAmount() != null && task.plannedAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💰 Rejalashtirilgan to‘lov: <b>").append(MoneyFormatter.format(task.plannedAmount())).append("</b>\n");
        }

        if (task.recurrenceType() != null && task.recurrenceType() != TodoRecurrenceType.NONE) {
            sb.append("🔄 Takrorlanish: <i>").append(task.recurrenceType().getDisplayName()).append("</i>\n");
        }

        return sb.toString();
    }

    public static String buildTaskListMessage(String filterName, List<TodoTaskDto> tasks, int page, int totalPages) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 <b>VAZIFALAR: ").append(filterName.toUpperCase()).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (tasks.isEmpty()) {
            sb.append("<i>Bu bo‘limda hozircha hech qanday vazifa yo‘q.</i>\n");
            return sb.toString();
        }

        for (int i = 0; i < tasks.size(); i++) {
            TodoTaskDto t = tasks.get(i);
            sb.append("<b>").append(i + 1).append(".</b> ");
            if (t.status() == TodoStatus.COMPLETED) {
                sb.append("<s>").append(escapeHtml(t.title())).append("</s> ✅\n");
            } else {
                sb.append(t.priority().getEmoji()).append(" <b>").append(escapeHtml(t.title())).append("</b>");
                if (t.isOverdue()) {
                    sb.append(" <i>(⚠️ Kechikkan)</i>");
                }
                sb.append("\n");
            }

            if (t.dueDate() != null) {
                sb.append("    📅 ").append(DateTimeUtils.formatUzbekDate(t.dueDate()));
                if (t.hasSpecificTime() && t.dueTime() != null) {
                    sb.append(" ").append(t.dueTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            }
            if (t.plannedAmount() != null) {
                sb.append(" | 💰 <code>").append(MoneyFormatter.format(t.plannedAmount())).append("</code>");
            }
            sb.append("\n\n");
        }

        sb.append("<i>Batafsil ko‘rish yoki boshqarish uchun pastdagi raqamni bosing:</i>");
        return sb.toString();
    }

    public static String buildProjectListMessage(List<ProjectSummaryDto> projects) {
        StringBuilder sb = new StringBuilder();
        sb.append("📁 <b>LOYIHALAR RO‘YXATI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (projects.isEmpty()) {
            sb.append("<i>Sizda hali loyihalar mavjud emas. Yangi loyiha yaratishingiz mumkin!</i>\n");
            return sb.toString();
        }

        for (int i = 0; i < projects.size(); i++) {
            ProjectSummaryDto p = projects.get(i);
            sb.append("<b>").append(i + 1).append(".</b> ").append(p.color()).append(" <b>").append(escapeHtml(p.name())).append("</b>\n");
            sb.append("   • Vazifalar: <b>").append(p.completedTasks()).append("/").append(p.totalTasks()).append("</b> (").append(p.getProgressPercent()).append("%)\n");
            sb.append("   • ").append(renderProgressBar(p.getProgressPercent())).append("\n");
            if (p.plannedAmount() != null && p.plannedAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("   • Reja: <code>").append(MoneyFormatter.format(p.plannedAmount())).append("</code>");
                if (p.actualAmount() != null && p.actualAmount().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append(" | Sarf: <code>").append(MoneyFormatter.format(p.actualAmount())).append("</code>");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String renderProgressBar(int percent) {
        int filled = Math.min(10, Math.max(0, percent / 10));
        return "[" + "█".repeat(filled) + "░".repeat(10 - filled) + "] " + percent + "%";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
