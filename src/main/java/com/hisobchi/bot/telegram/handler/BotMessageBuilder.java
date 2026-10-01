package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.statistics.dto.CategoryExpenseDto;
import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.dto.MonthlyStatisticsDto;
import com.hisobchi.bot.statistics.dto.WeeklyStatisticsDto;
import com.hisobchi.bot.transaction.dto.DraftDto;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BotMessageBuilder {

    private BotMessageBuilder() {}

    public static String buildWelcomeMessage() {
        return """
                👋 <b>Hisobchi Bot'ga xush kelibsiz!</b>

                Bu bot orqali kunlik:
                💰 daromadingizni
                💸 xarajatlaringizni
                📊 sof foydangizni

                oson va aniq nazorat qilishingiz mumkin.

                Xarajat yoki daromadni yozib yoki ovoz bilan yuborishingiz mumkin.

                Masalan:
                🎙 <i>"15 ming obedga"</i>
                yoki
                ✍️ <i>"300 ming benzin"</i>
                yoki
                💰 <i>"2 million montajdan ishladim"</i>
                """;
    }

    public static String buildDraftConfirmationMessage(DraftDto draft, String timezone) {
        StringBuilder sb = new StringBuilder();

        if (draft.source() == TransactionSource.VOICE) {
            sb.append("🎙 <b>Ovozdan aniqlandi:</b>\n");
            if (draft.originalText() != null && !draft.originalText().isBlank()) {
                sb.append("<i>\"").append(escapeHtml(draft.originalText())).append("\"</i>\n\n");
            }
        } else if (draft.source() == TransactionSource.TEXT) {
            sb.append("✍️ <b>Matndan aniqlandi:</b>\n\n");
        }

        if (draft.type() == TransactionType.INCOME) {
            sb.append("💰 <b>Yangi daromad</b>\n\n");
            sb.append("💵 Summa:\n<b>").append(MoneyFormatter.format(draft.amount())).append("</b>\n\n");
            sb.append("📌 Manba:\n").append(draft.categoryName() != null ? draft.getCategoryDisplayName() : "Aniqlanmagan").append("\n\n");
        } else {
            sb.append("💸 <b>Yangi xarajat</b>\n\n");
            sb.append("💰 Summa:\n<b>").append(MoneyFormatter.format(draft.amount())).append("</b>\n\n");
            sb.append("📂 Kategoriya:\n").append(draft.categoryName() != null ? draft.getCategoryDisplayName() : "Aniqlanmagan").append("\n\n");
        }

        if (draft.description() != null && !draft.description().isBlank()) {
            sb.append("📝 Izoh:\n").append(escapeHtml(draft.description())).append("\n\n");
        }

        sb.append("📅 Sana:\n").append(DateTimeUtils.formatDateTime(draft.createdAt(), timezone)).append("\n\n");
        sb.append("<b>Ma’lumot to‘g‘rimi?</b>");
        return sb.toString();
    }

    public static String buildSaveSuccessMessage(TransactionDto tx, BigDecimal todayTotalExpense, BigDecimal todayTotalIncome, BigDecimal todayNetProfit) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ <b>Saqlandi</b>\n\n");

        if (tx.type() == TransactionType.INCOME) {
            sb.append("💰 +").append(MoneyFormatter.format(tx.amount())).append("\n");
            sb.append("📌 ").append(tx.getCategoryDisplayName()).append("\n\n");
            sb.append("Bugungi jami daromad:\n<b>").append(MoneyFormatter.format(todayTotalIncome)).append("</b>\n\n");
            sb.append("Bugungi sof foyda:\n<b>").append(MoneyFormatter.format(todayNetProfit)).append("</b>");
        } else {
            sb.append("💸 ").append(MoneyFormatter.format(tx.amount())).append("\n");
            sb.append(tx.getCategoryDisplayName()).append("\n\n");
            sb.append("Bugungi jami xarajat:\n<b>").append(MoneyFormatter.format(todayTotalExpense)).append("</b>\n\n");
            sb.append("Bugungi sof foyda:\n<b>").append(MoneyFormatter.format(todayNetProfit)).append("</b>");
        }
        return sb.toString();
    }

    public static String buildDailyStatisticsMessage(DailyStatisticsDto stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>BUGUNGI HISOBOT</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("📅 ").append(DateTimeUtils.formatUzbekDate(stats.date())).append("\n\n");
        sb.append("💰 <b>Daromad:</b>\n").append(MoneyFormatter.format(stats.totalIncome())).append("\n\n");
        sb.append("💸 <b>Xarajat:</b>\n").append(MoneyFormatter.format(stats.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>SOF FOYDA:</b>\n<b>").append(MoneyFormatter.format(stats.netProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (stats.expenseCategories() != null && !stats.expenseCategories().isEmpty()) {
            sb.append("📂 <b>XARAJATLAR:</b>\n\n");
            for (CategoryExpenseDto cat : stats.expenseCategories()) {
                sb.append(cat.getDisplayName()).append("\n")
                        .append(MoneyFormatter.format(cat.totalAmount())).append("\n\n");
            }
            sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        }

        sb.append("🧾 Operatsiyalar: ").append(stats.transactionCount()).append(" ta\n");
        if (stats.isClosed()) {
            sb.append("🔐 <i>Kun yopilgan</i>\n");
        }
        return sb.toString();
    }

    public static String buildWeeklyStatisticsMessage(WeeklyStatisticsDto stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 <b>HAFTALIK HISOBOT</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append(DateTimeUtils.formatUzbekDateRange(stats.startDate(), stats.endDate())).append("\n\n");
        sb.append("💰 <b>Jami daromad:</b>\n").append(MoneyFormatter.format(stats.totalIncome())).append("\n\n");
        sb.append("💸 <b>Jami xarajat:</b>\n").append(MoneyFormatter.format(stats.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>Sof foyda:</b>\n<b>").append(MoneyFormatter.format(stats.netProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (stats.expenseCategories() != null && !stats.expenseCategories().isEmpty()) {
            sb.append("📊 <b>Xarajat taqsimoti:</b>\n\n");
            for (CategoryExpenseDto cat : stats.expenseCategories()) {
                sb.append(cat.getDisplayName()).append(" — ")
                        .append(MoneyFormatter.format(cat.totalAmount())).append("\n");
            }
            sb.append("\n");
        }

        if (stats.topExpenseCategory() != null) {
            sb.append("📈 <b>Eng ko‘p xarajat:</b>\n")
                    .append(stats.topExpenseCategory().getDisplayName()).append(" (")
                    .append(MoneyFormatter.format(stats.topExpenseCategory().totalAmount())).append(")\n\n");
        }

        sb.append("🧾 Jami operatsiyalar: ").append(stats.transactionCount()).append(" ta");
        return sb.toString();
    }

    public static String buildMonthlyStatisticsMessage(MonthlyStatisticsDto stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("🗓 <b>").append(DateTimeUtils.formatUzbekMonthYear(stats.monthDate())).append(" HISOBOTI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("💰 <b>Jami ishladingiz:</b>\n").append(MoneyFormatter.format(stats.totalIncome())).append("\n\n");
        sb.append("💸 <b>Jami xarajat:</b>\n").append(MoneyFormatter.format(stats.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>Sof foyda:</b>\n<b>").append(MoneyFormatter.format(stats.netProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (stats.expenseCategories() != null && !stats.expenseCategories().isEmpty()) {
            sb.append("📂 <b>Xarajatlar:</b>\n\n");
            for (CategoryExpenseDto cat : stats.expenseCategories()) {
                sb.append(cat.getDisplayName()).append("\n")
                        .append(MoneyFormatter.format(cat.totalAmount())).append("\n\n");
            }
            sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        }

        sb.append("📊 Ishlangan kun: ").append(stats.activeDays()).append(" kun\n\n");
        sb.append("📅 O‘rtacha kunlik daromad:\n<b>").append(MoneyFormatter.format(stats.averageDailyIncome())).append("</b>\n\n");
        sb.append("💸 O‘rtacha kunlik xarajat:\n<b>").append(MoneyFormatter.format(stats.averageDailyExpense())).append("</b>\n\n");
        sb.append("✅ O‘rtacha sof foyda:\n<b>").append(MoneyFormatter.format(stats.averageDailyNetProfit())).append("</b>\n");
        return sb.toString();
    }

    public static String buildDayTransactionsHistory(LocalDate date, List<TransactionDto> list, String timezone) {
        if (list == null || list.isEmpty()) {
            return "📅 <b>" + DateTimeUtils.formatDate(date) + "</b>\n\n<i>Bu sana bo‘yicha operatsiyalar yo‘q.</i>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📅 <b>").append(DateTimeUtils.formatDate(date)).append("</b>\n\n");

        for (TransactionDto tx : list) {
            sb.append("⏱ ").append(DateTimeUtils.formatTime(tx.createdAt(), timezone)).append("\n");
            if (tx.type() == TransactionType.INCOME) {
                sb.append("💰 <b>+").append(MoneyFormatter.format(tx.amount())).append("</b>\n");
            } else {
                sb.append("💸 <b>-").append(MoneyFormatter.format(tx.amount())).append("</b>\n");
            }
            sb.append(tx.getCategoryDisplayName()).append("\n");
            if (tx.description() != null && !tx.description().isBlank()) {
                sb.append("<i>").append(escapeHtml(tx.description())).append("</i>\n");
            }
            sb.append("/tx_").append(tx.id()).append(" (batafsil)\n\n");
        }

        return sb.toString();
    }

    public static String buildTransactionDetail(TransactionDto tx, String timezone) {
        StringBuilder sb = new StringBuilder();
        sb.append("🧾 <b>Operatsiya #").append(tx.id()).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (tx.type() == TransactionType.INCOME) {
            sb.append("💰 Turi: <b>Daromad</b>\n");
        } else {
            sb.append("💸 Turi: <b>Xarajat</b>\n");
        }

        sb.append("💵 Summa: <b>").append(MoneyFormatter.format(tx.amount())).append("</b>\n");
        sb.append("📂 Kategoriya: <b>").append(tx.getCategoryDisplayName()).append("</b>\n");

        if (tx.description() != null && !tx.description().isBlank()) {
            sb.append("📝 Izoh: ").append(escapeHtml(tx.description())).append("\n");
        }

        sb.append("📅 Sana: ").append(DateTimeUtils.formatDate(tx.transactionDate())).append("\n");
        sb.append("⏱ Yozilgan vaqti: ").append(DateTimeUtils.formatDateTime(tx.createdAt(), timezone)).append("\n");
        return sb.toString();
    }

    public static String buildDebtDraftConfirmationMessage(com.hisobchi.bot.debt.entity.DebtDraft draft) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤝 <b>QARZNI TEKSHIRING</b>\n\n");

        boolean isBorrowed = draft.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED;
        if (isBorrowed) {
            sb.append("🔴 <b>Siz qarz oldingiz</b>\n\n");
        } else {
            sb.append("🟢 <b>Siz qarz berdingiz</b>\n\n");
        }

        sb.append("💰 <b>Summa:</b>\n").append(MoneyFormatter.format(draft.getAmount())).append("\n\n");

        if (isBorrowed) {
            sb.append("👤 <b>Kimdan:</b>\n").append(escapeHtml(draft.getPersonName())).append("\n\n");
            sb.append("📅 <b>Olingan sana:</b>\n").append(draft.getBorrowedOrLentDate() != null ? draft.getBorrowedOrLentDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Bugun").append("\n\n");
        } else {
            sb.append("👤 <b>Kimga:</b>\n").append(escapeHtml(draft.getPersonName())).append("\n\n");
            sb.append("📅 <b>Berilgan sana:</b>\n").append(draft.getBorrowedOrLentDate() != null ? draft.getBorrowedOrLentDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Bugun").append("\n\n");
        }

        if (draft.getDueDate() != null) {
            sb.append("⏰ <b>Qaytarish sanasi:</b>\n").append(draft.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");
        } else {
            sb.append("⏰ <b>Qaytarish sanasi:</b>\n<i>Belgilanmagan</i>\n\n");
        }

        sb.append("💳 <b>Pul turi:</b>\n").append(draft.getPaymentMethod() != null ? draft.getPaymentMethod() : "Naqd").append("\n\n");

        sb.append("<b>Saqlaymizmi?</b>");
        return sb.toString();
    }

    public static String buildActiveDebtsOverviewMessage(
            List<com.hisobchi.bot.debt.entity.Debt> borrowed,
            List<com.hisobchi.bot.debt.entity.Debt> lent) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤝 <b>FAOL QARZLAR</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        LocalDate today = LocalDate.now();
        java.time.format.DateTimeFormatter shortFmt = java.time.format.DateTimeFormatter.ofPattern("d-MMMM", java.util.Locale.forLanguageTag("uz-UZ"));

        sb.append("🔴 <b>MEN OLGAN QARZLAR</b>\n\n");
        if (borrowed.isEmpty()) {
            sb.append("<i>Olingan qarzlar yo‘q</i>\n\n");
        } else {
            int i = 1;
            for (com.hisobchi.bot.debt.entity.Debt d : borrowed) {
                sb.append(i++).append(". <b>").append(escapeHtml(d.getPersonName())).append("</b>\n");
                sb.append(MoneyFormatter.format(d.getAmount())).append("\n");
                if (d.getDueDate() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getDueDate());
                    String daysText = days < 0 ? "⚠️ Muddati o‘tgan!" : (days == 0 ? "⚠️ Bugun to‘lash kuni!" : days + " kun qoldi");
                    sb.append("⏰ ").append(d.getDueDate().format(shortFmt)).append("\n");
                    sb.append(daysText).append("\n");
                }
                sb.append("👉 /debt_view_").append(d.getId()).append("\n\n");
            }
        }

        sb.append("🟢 <b>MEN BERGAN QARZLAR</b>\n\n");
        if (lent.isEmpty()) {
            sb.append("<i>Berilgan qarzlar yo‘q</i>\n");
        } else {
            int i = 1;
            for (com.hisobchi.bot.debt.entity.Debt d : lent) {
                sb.append(i++).append(". <b>").append(escapeHtml(d.getPersonName())).append("</b>\n");
                sb.append(MoneyFormatter.format(d.getAmount())).append("\n");
                if (d.getDueDate() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getDueDate());
                    String daysText = days < 0 ? "⚠️ Muddati o‘tgan!" : (days == 0 ? "🤝 Bugun qaytarish kuni!" : days + " kun qoldi");
                    sb.append("⏰ ").append(d.getDueDate().format(shortFmt)).append("\n");
                    sb.append(daysText).append("\n");
                }
                sb.append("👉 /debt_view_").append(d.getId()).append("\n\n");
            }
        }

        return sb.toString();
    }

    public static String buildClosedDebtsMessage(List<com.hisobchi.bot.debt.entity.Debt> closed) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ <b>YOPILGAN QARZLAR</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (closed.isEmpty()) {
            sb.append("<i>Yopilgan qarzlar mavjud emas.</i>\n");
            return sb.toString();
        }

        int i = 1;
        for (com.hisobchi.bot.debt.entity.Debt d : closed) {
            String icon = d.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED ? "🔴" : "🟢";
            String status = d.getStatus() == com.hisobchi.bot.debt.entity.DebtStatus.PAID ? "To‘langan" : "Qaytarib olingan";
            sb.append(icon).append(" ").append(i++).append(". <b>").append(escapeHtml(d.getPersonName())).append("</b> — ")
                    .append(MoneyFormatter.format(d.getAmount()))
                    .append(" (<i>").append(status).append("</i>)\n");
        }
        return sb.toString();
    }

    public static String buildDebtDetailMessage(com.hisobchi.bot.debt.entity.Debt d) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤝 <b>QARZ MA’LUMOTI</b>\n\n");

        if (d.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED) {
            sb.append("🔴 <b>Siz qarz olgansiz</b>\n\n");
            sb.append("👤 <b>Kimdan:</b> ").append(escapeHtml(d.getPersonName())).append("\n");
        } else {
            sb.append("🟢 <b>Siz qarz bergansiz</b>\n\n");
            sb.append("👤 <b>Kimga:</b> ").append(escapeHtml(d.getPersonName())).append("\n");
        }

        sb.append("💰 <b>Summa:</b> ").append(MoneyFormatter.format(d.getAmount())).append("\n\n");

        java.time.format.DateTimeFormatter dFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
        if (d.getBorrowedOrLentDate() != null) {
            sb.append("📅 <b>Sana:</b> ").append(d.getBorrowedOrLentDate().format(dFmt)).append("\n");
        }
        if (d.getDueDate() != null) {
            sb.append("⏰ <b>Qaytarish:</b> ").append(d.getDueDate().format(dFmt)).append("\n");
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), d.getDueDate());
            if (days < 0) {
                sb.append("⏳ <b>Holat:</b> ⚠️ Muddati o‘tgan (").append(Math.abs(days)).append(" kun oldin)\n");
            } else if (days == 0) {
                sb.append("⏳ <b>Holat:</b> ⚠️ Bugun qaytarish kuni!\n");
            } else {
                sb.append("⏳ <b>Qoldi:</b> ").append(days).append(" kun\n");
            }
        }

        sb.append("💳 <b>Pul turi:</b> ").append(d.getPaymentMethod() != null ? d.getPaymentMethod() : "Naqd").append("\n");

        String statusStr = switch (d.getStatus()) {
            case ACTIVE -> "Faol";
            case PAID -> "✅ To‘langan";
            case RECEIVED -> "✅ Qaytarib olingan";
            case OVERDUE -> "⚠️ Muddati o‘tgan";
            case CANCELLED -> "Bekor qilingan";
        };
        sb.append("📌 <b>Status:</b> ").append(statusStr).append("\n");

        if (d.getDescription() != null && !d.getDescription().isBlank()) {
            sb.append("📝 <b>Izoh:</b> <i>").append(escapeHtml(d.getDescription())).append("</i>\n");
        }

        return sb.toString();
    }

    public static String buildDebtStatisticsMessage(com.hisobchi.bot.debt.dto.DebtStatisticsDto stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤝 <b>QARZLAR HISOBOTI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("🔴 <b>Siz to‘lashingiz kerak:</b>\n")
                .append(MoneyFormatter.format(stats.totalToPay())).append("\n\n");

        sb.append("🟢 <b>Sizga qaytarilishi kerak:</b>\n")
                .append(MoneyFormatter.format(stats.totalToReceive())).append("\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("🔴 <b>Faol olingan qarz:</b> ").append(stats.activeBorrowedCount()).append(" ta\n");
        sb.append("🟢 <b>Faol berilgan qarz:</b> ").append(stats.activeLentCount()).append(" ta\n");
        sb.append("⚠️ <b>Muddati o‘tgan:</b> ").append(stats.overdueCount()).append(" ta\n");

        return sb.toString();
    }

    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
