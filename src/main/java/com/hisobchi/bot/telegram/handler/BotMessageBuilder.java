package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.statistics.dto.CategoryExpenseDto;
import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.dto.MonthlyStatisticsDto;
import com.hisobchi.bot.statistics.dto.WeeklyStatisticsDto;
import com.hisobchi.bot.transaction.dto.DraftDto;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    public static String buildDraftConfirmationMessage(DraftDto draft, String timezone, boolean isOffDay, String lastWorkDayText, boolean hasEnteredProfitToday) {
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
        if (draft.type() == TransactionType.EXPENSE) {
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            if (hasEnteredProfitToday) {
                sb.append("🤔 <b>Bugungi foydadan minus qilinsinmi?</b>\n\n");
                sb.append("✅ <b>Ha</b> — kiritilgan kunlik foydadan ayiriladi, umumiy topilgan pul o‘zgarmaydi.\n");
                sb.append("❌ <b>Yo‘q</b> — alohida xarajat bo‘ladi, umumiy topilgan pulga va xarajatga qo‘shiladi.");
            } else if (isOffDay) {
                sb.append("🏖 <b>Bugun dam olish kuni.</b>\n");
                String dayLabel = (lastWorkDayText != null && !lastWorkDayText.isBlank()) ? lastWorkDayText : "oldingi ishlagan kun";
                sb.append("<i>Bu xarajat ").append(dayLabel).append(" foydasidan ayiriladi:</i>");
            } else {
                sb.append("🤔 <b>Bu xarajat qaysi foydadan qilindi?</b>\n");
                sb.append("<i>Oldingi yoki bugungi foydani tanlang:</i>");
            }
        } else {
            sb.append("<b>Ma’lumot to‘g‘rimi?</b>");
        }
        return sb.toString();
    }

    public static String buildDraftConfirmationMessage(DraftDto draft, String timezone, boolean isOffDay, String lastWorkDayText) {
        return buildDraftConfirmationMessage(draft, timezone, isOffDay, lastWorkDayText, false);
    }

    public static String buildDraftConfirmationMessage(DraftDto draft, String timezone) {
        return buildDraftConfirmationMessage(draft, timezone, false, null, false);
    }

    public static String buildSaveSuccessMessageWithSource(TransactionDto tx, boolean fromYesterday, String profitSourceLabel, BigDecimal totalExpense, BigDecimal totalIncome, BigDecimal netProfit) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ <b>Saqlandi</b>\n\n");
        String label = (profitSourceLabel != null && !profitSourceLabel.isBlank()) ? profitSourceLabel : (fromYesterday ? "Oldingi ishlagan kun" : "Bugungi");
        if (fromYesterday) {
            sb.append("💸 <b>").append(MoneyFormatter.format(tx.amount())).append("</b> (").append(label).append(" foydasidan ayirildi)\n");
            sb.append("📌 ").append(tx.getCategoryDisplayName()).append("\n\n");
            sb.append("💸 ").append(label).append(" umumiy xarajatlar:\n<b>").append(MoneyFormatter.format(totalExpense)).append("</b>\n\n");
            sb.append("✅ ").append(label).append(" yakuniy sof foyda:\n<b>").append(MoneyFormatter.format(netProfit)).append("</b>");
        } else {
            sb.append("💸 <b>").append(MoneyFormatter.format(tx.amount())).append("</b> (Bugungi foydadan minus qilindi)\n");
            sb.append("📌 ").append(tx.getCategoryDisplayName()).append("\n\n");
            sb.append("💰 Bugungi umumiy topilgan pul (o‘zgarmadi):\n<b>").append(MoneyFormatter.format(totalIncome)).append("</b>\n\n");
            sb.append("💸 Bugungi umumiy xarajatlar:\n<b>").append(MoneyFormatter.format(totalExpense)).append("</b>\n\n");
            sb.append("✅ Bugungi qolgan foyda:\n<b>").append(MoneyFormatter.format(netProfit)).append("</b>");
        }
        return sb.toString();
    }

    public static String buildSaveSuccessMessageWithSource(TransactionDto tx, boolean fromYesterday, BigDecimal totalExpense, BigDecimal totalIncome, BigDecimal netProfit) {
        return buildSaveSuccessMessageWithSource(tx, fromYesterday, fromYesterday ? "Kechagi" : "Bugungi", totalExpense, totalIncome, netProfit);
    }

    public static String buildSaveSuccessMessage(TransactionDto tx, BigDecimal todayTotalExpense, BigDecimal todayTotalIncome, BigDecimal todayNetProfit) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ <b>Saqlandi</b>\n\n");

        if (tx.type() == TransactionType.INCOME) {
            sb.append("💰 +").append(MoneyFormatter.format(tx.amount())).append("\n");
            sb.append("📌 ").append(tx.getCategoryDisplayName()).append("\n\n");
            sb.append("💰 Bugungi umumiy ishlab topilgan:\n<b>").append(MoneyFormatter.format(todayTotalIncome)).append("</b>\n\n");
            sb.append("✅ Bugungi foydangiz:\n<b>").append(MoneyFormatter.format(todayNetProfit)).append("</b>");
        } else {
            sb.append("💸 ").append(MoneyFormatter.format(tx.amount())).append("\n");
            sb.append(tx.getCategoryDisplayName()).append("\n\n");
            sb.append("💸 Bugungi xarajatlar:\n<b>").append(MoneyFormatter.format(todayTotalExpense)).append("</b>\n\n");
            sb.append("✅ Bugungi foydangiz:\n<b>").append(MoneyFormatter.format(todayNetProfit)).append("</b>");
        }
        return sb.toString();
    }

    public static String buildDailyStatisticsMessage(DailyStatisticsDto stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>BUGUNGI HISOBOT</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("📅 ").append(DateTimeUtils.formatUzbekDate(stats.date()));
        if (stats.isOffDay()) {
            sb.append(" (🏖 Dam olish kuni)");
        }
        sb.append("\n\n");
        sb.append("💰 <b>Umumiy ishlab topilgan:</b>\n").append(MoneyFormatter.format(stats.totalIncome())).append("\n\n");
        sb.append("💸 <b>Xarajatlar:</b>\n").append(MoneyFormatter.format(stats.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>BUGUNGI FOYDANGIZ:</b>\n<b>").append(MoneyFormatter.format(stats.netProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (stats.expenseCategories() != null && !stats.expenseCategories().isEmpty()) {
            sb.append("📁 <b>XARAJATLAR:</b>\n\n");
            for (CategoryExpenseDto cat : stats.expenseCategories()) {
                sb.append(cat.getDisplayName()).append("\n")
                        .append(MoneyFormatter.format(cat.totalAmount())).append("\n\n");
            }
            sb.append("━━━━━━━━━━━━━━━━━━\n");
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
        sb.append("💰 <b>Umumiy ishlab topilgan:</b>\n").append(MoneyFormatter.format(stats.totalIncome())).append("\n\n");
        sb.append("💸 <b>Xarajatlar:</b>\n").append(MoneyFormatter.format(stats.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>HAFTALIK FOYDANGIZ:</b>\n<b>").append(MoneyFormatter.format(stats.netProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (stats.expenseCategories() != null && !stats.expenseCategories().isEmpty()) {
            sb.append("📁 <b>XARAJATLAR:</b>\n\n");
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
        sb.append("💰 <b>Umumiy ishlab topilgan:</b>\n").append(MoneyFormatter.format(stats.totalIncome())).append("\n\n");
        sb.append("💸 <b>Xarajatlar:</b>\n").append(MoneyFormatter.format(stats.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>OYLIK FOYDANGIZ:</b>\n<b>").append(MoneyFormatter.format(stats.netProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (stats.cashProfit() != null && stats.cardProfit() != null
                && (stats.cashProfit().compareTo(BigDecimal.ZERO) > 0 || stats.cardProfit().compareTo(BigDecimal.ZERO) > 0)) {
            sb.append("💵 <b>Naqd:</b> ").append(MoneyFormatter.format(stats.cashProfit())).append("\n");
            sb.append("💳 <b>Karta:</b> ").append(MoneyFormatter.format(stats.cardProfit())).append("\n\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        }

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

    public static String buildDayTransactionsHistoryDetailed(LocalDate date, List<Transaction> list, String timezone) {
        return buildDayTransactionsHistoryDetailed(date, list, List.of(), List.of(), null, timezone);
    }

    public static String buildDayTransactionsHistoryDetailed(
            LocalDate date,
            List<Transaction> list,
            List<com.hisobchi.bot.debt.entity.Debt> debts,
            List<com.hisobchi.bot.debt.entity.DebtPayment> payments,
            com.hisobchi.bot.profit.entity.DailyProfit profit,
            String timezone) {

        boolean hasTxs = list != null && !list.isEmpty();
        boolean hasDebts = debts != null && !debts.isEmpty();
        boolean hasPayments = payments != null && !payments.isEmpty();
        boolean hasProfit = profit != null && profit.getTotalProfit() != null && profit.getTotalProfit().compareTo(BigDecimal.ZERO) > 0;

        if (!hasTxs && !hasDebts && !hasPayments && !hasProfit) {
            return "ℹ️ <b>" + DateTimeUtils.formatUzbekDate(date) + "</b> kuni hech qanday operatsiya topilmadi.";
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int opCount = 0;

        if (hasProfit) {
            income = income.add(profit.getTotalProfit());
            opCount++;
        }

        if (hasTxs) {
            for (Transaction tx : list) {
                if (tx.getType() == TransactionType.INCOME) {
                    income = income.add(tx.getAmount());
                } else {
                    expense = expense.add(tx.getAmount());
                }
                opCount++;
            }
        }

        if (hasDebts) {
            for (com.hisobchi.bot.debt.entity.Debt d : debts) {
                BigDecimal amt = d.getOriginalAmount() != null ? d.getOriginalAmount() : d.getAmount();
                if (d.getType() == com.hisobchi.bot.debt.entity.DebtType.LENT) {
                    expense = expense.add(amt);
                } else {
                    income = income.add(amt);
                }
                opCount++;
            }
        }

        if (hasPayments) {
            for (com.hisobchi.bot.debt.entity.DebtPayment p : payments) {
                if (p.getPaymentType() == com.hisobchi.bot.debt.entity.DebtPaymentType.DEBT_PAYMENT) {
                    expense = expense.add(p.getAmount());
                } else {
                    income = income.add(p.getAmount());
                }
                opCount++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📜 <b>OPERATSIYALAR TARIXI</b>\n");
        sb.append("📅 <b>").append(DateTimeUtils.formatUzbekDate(date)).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💰 Jami daromad: <b>+").append(MoneyFormatter.format(income)).append("</b>\n");
        }
        sb.append("💸 Jami xarajat: <b>-").append(MoneyFormatter.format(expense)).append("</b>\n");
        sb.append("🧾 Jami operatsiyalar: <b>").append(opCount).append(" ta</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (hasProfit) {
            sb.append("💵 <b>KUNLIK FOYDA:</b>\n");
            sb.append("  • Foyda: <b>").append(MoneyFormatter.format(profit.getTotalProfit())).append("</b>");
            if (profit.getCashAmount() != null || profit.getCardAmount() != null) {
                sb.append(" (💵 Naqd: ").append(MoneyFormatter.format(profit.getCashAmount() != null ? profit.getCashAmount() : BigDecimal.ZERO))
                  .append(" | 💳 Karta: ").append(MoneyFormatter.format(profit.getCardAmount() != null ? profit.getCardAmount() : BigDecimal.ZERO)).append(")");
            }
            sb.append("\n\n");
        }

        int idx = 1;
        if (hasTxs) {
            sb.append("📁 <b>XARAJAT VA DAROMADLAR:</b>\n");
            for (Transaction tx : list) {
                String emoji = (tx.getCategory() != null && tx.getCategory().getEmoji() != null)
                        ? tx.getCategory().getEmoji() : "📌";
                String catName = (tx.getCategory() != null) ? tx.getCategory().getName() : "Boshqa";
                String sign = tx.getType() == TransactionType.INCOME ? "+" : "-";
                String timeStr = DateTimeUtils.formatTime(tx.getCreatedAt(), timezone);

                sb.append(idx++).append(". ").append(emoji).append(" <b>").append(escapeHtml(catName)).append("</b>: ");
                sb.append("<b>").append(sign).append(MoneyFormatter.format(tx.getAmount())).append("</b>\n");
                sb.append("   ⏱ <i>").append(timeStr).append("</i>");
                if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
                    sb.append(" • <i>").append(escapeHtml(tx.getDescription())).append("</i>");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (hasDebts || hasPayments) {
            sb.append("🤝 <b>QARZ HARAKATLARI:</b>\n");
            if (hasDebts) {
                for (com.hisobchi.bot.debt.entity.Debt d : debts) {
                    String timeStr = d.getCreatedAt() != null ? DateTimeUtils.formatTime(d.getCreatedAt(), timezone) : "";
                    String sign = d.getType() == com.hisobchi.bot.debt.entity.DebtType.LENT ? "-" : "+";
                    String title = d.getType() == com.hisobchi.bot.debt.entity.DebtType.LENT ? "💸 Qarz berildi" : "💰 Qarz olindi";
                    BigDecimal amt = d.getOriginalAmount() != null ? d.getOriginalAmount() : d.getAmount();
                    sb.append(idx++).append(". ").append(title).append(" (<b>").append(escapeHtml(d.getPersonName())).append("</b>): ");
                    sb.append("<b>").append(sign).append(MoneyFormatter.format(amt)).append("</b>\n");
                    if (!timeStr.isBlank()) {
                        sb.append("   ⏱ <i>").append(timeStr).append("</i> • <i>").append(d.getPaymentMethod() != null ? d.getPaymentMethod() : "Naqd").append("</i>\n");
                    }
                }
            }
            if (hasPayments) {
                for (com.hisobchi.bot.debt.entity.DebtPayment p : payments) {
                    String timeStr = p.getCreatedAt() != null ? DateTimeUtils.formatTime(p.getCreatedAt(), timezone) : "";
                    boolean isPayment = p.getPaymentType() == com.hisobchi.bot.debt.entity.DebtPaymentType.DEBT_PAYMENT;
                    String sign = isPayment ? "-" : "+";
                    String title = isPayment ? "💳 Qarz to‘landi" : "💵 Qarz qaytdi";
                    String person = p.getDebt() != null ? p.getDebt().getPersonName() : "";
                    sb.append(idx++).append(". ").append(title);
                    if (!person.isBlank()) {
                        sb.append(" (<b>").append(escapeHtml(person)).append("</b>)");
                    }
                    sb.append(": <b>").append(sign).append(MoneyFormatter.format(p.getAmount())).append("</b>\n");
                    if (!timeStr.isBlank()) {
                        sb.append("   ⏱ <i>").append(timeStr).append("</i> • <i>").append(p.getPaymentMethod() != null ? p.getPaymentMethod() : "Naqd").append("</i>\n");
                    }
                }
            }
            sb.append("\n");
        }

        sb.append("<i>Tahrirlash yoki ko‘rish uchun kerakli operatsiya tugmasini bosing:</i>");
        return sb.toString();
    }

    public static String buildPeriodTransactionsHistory(String title, LocalDate start, LocalDate end, List<Transaction> list, String timezone) {
        return buildPeriodTransactionsHistory(title, start, end, list, List.of(), List.of(), List.of(), timezone);
    }

    public static String buildPeriodTransactionsHistory(
            String title,
            LocalDate start,
            LocalDate end,
            List<Transaction> list,
            List<com.hisobchi.bot.debt.entity.Debt> debts,
            List<com.hisobchi.bot.debt.entity.DebtPayment> payments,
            List<com.hisobchi.bot.profit.entity.DailyProfit> profits,
            String timezone) {

        boolean hasTxs = list != null && !list.isEmpty();
        boolean hasDebts = debts != null && !debts.isEmpty();
        boolean hasPayments = payments != null && !payments.isEmpty();
        boolean hasProfits = profits != null && profits.stream().anyMatch(p -> p.getTotalProfit() != null && p.getTotalProfit().compareTo(BigDecimal.ZERO) > 0);

        if (!hasTxs && !hasDebts && !hasPayments && !hasProfits) {
            return "ℹ️ <b>" + title + "</b> (" + DateTimeUtils.formatUzbekDateRange(start, end) + ") bo‘yicha hech qanday operatsiya topilmadi.";
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int opCount = 0;

        if (hasProfits) {
            for (com.hisobchi.bot.profit.entity.DailyProfit p : profits) {
                if (p.getTotalProfit() != null) {
                    income = income.add(p.getTotalProfit());
                    opCount++;
                }
            }
        }

        if (hasTxs) {
            for (Transaction tx : list) {
                if (tx.getType() == TransactionType.INCOME) {
                    income = income.add(tx.getAmount());
                } else {
                    expense = expense.add(tx.getAmount());
                }
                opCount++;
            }
        }

        if (hasDebts) {
            for (com.hisobchi.bot.debt.entity.Debt d : debts) {
                BigDecimal amt = d.getOriginalAmount() != null ? d.getOriginalAmount() : d.getAmount();
                if (d.getType() == com.hisobchi.bot.debt.entity.DebtType.LENT) {
                    expense = expense.add(amt);
                } else {
                    income = income.add(amt);
                }
                opCount++;
            }
        }

        if (hasPayments) {
            for (com.hisobchi.bot.debt.entity.DebtPayment p : payments) {
                if (p.getPaymentType() == com.hisobchi.bot.debt.entity.DebtPaymentType.DEBT_PAYMENT) {
                    expense = expense.add(p.getAmount());
                } else {
                    income = income.add(p.getAmount());
                }
                opCount++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📜 <b>").append(title.toUpperCase()).append(" TARIXI</b>\n");
        sb.append("📅 ").append(DateTimeUtils.formatUzbekDateRange(start, end)).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💰 Jami daromad: <b>+").append(MoneyFormatter.format(income)).append("</b>\n");
        }
        sb.append("💸 Jami xarajat: <b>-").append(MoneyFormatter.format(expense)).append("</b>\n");
        sb.append("🧾 Jami operatsiyalar: <b>").append(opCount).append(" ta</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (hasTxs) {
            LocalDate currentDate = null;
            int count = 0;
            for (Transaction tx : list) {
                if (count >= 20) {
                    sb.append("<i>... va yana ").append(list.size() - count).append(" ta xarajat operatsiyasi</i>\n");
                    break;
                }
                if (!tx.getTransactionDate().equals(currentDate)) {
                    currentDate = tx.getTransactionDate();
                    sb.append("📅 <b>").append(DateTimeUtils.formatUzbekDate(currentDate)).append("</b>\n");
                }

                String emoji = (tx.getCategory() != null && tx.getCategory().getEmoji() != null)
                        ? tx.getCategory().getEmoji() : "📌";
                String catName = (tx.getCategory() != null) ? tx.getCategory().getName() : "Boshqa";
                String sign = tx.getType() == TransactionType.INCOME ? "+" : "-";
                String timeStr = DateTimeUtils.formatTime(tx.getCreatedAt(), timezone);

                sb.append("  • ").append(timeStr).append(" | ").append(emoji).append(" ").append(escapeHtml(catName))
                  .append(": <b>").append(sign).append(MoneyFormatter.format(tx.getAmount())).append("</b>");
                if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
                    sb.append(" (").append(escapeHtml(tx.getDescription())).append(")");
                }
                sb.append("\n");
                count++;
            }
            sb.append("\n");
        }

        if (hasDebts) {
            sb.append("🤝 <b>BERILGAN / OLINGAN QARZLAR:</b>\n");
            int dCount = 0;
            for (com.hisobchi.bot.debt.entity.Debt d : debts) {
                if (dCount >= 10) break;
                String icon = d.getType() == com.hisobchi.bot.debt.entity.DebtType.LENT ? "💸 Qarz berildi" : "💰 Qarz olindi";
                String sign = d.getType() == com.hisobchi.bot.debt.entity.DebtType.LENT ? "-" : "+";
                BigDecimal amt = d.getOriginalAmount() != null ? d.getOriginalAmount() : d.getAmount();
                sb.append("  • ").append(icon).append(" (<b>").append(escapeHtml(d.getPersonName())).append("</b>): ")
                  .append("<b>").append(sign).append(MoneyFormatter.format(amt)).append("</b>\n");
                dCount++;
            }
            sb.append("\n");
        }

        sb.append("<i>Tahrirlash yoki o‘chirish uchun kerakli operatsiya tugmasini bosing:</i>");
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
        boolean isBorrowed = draft.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED;
        java.time.format.DateTimeFormatter dFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");

        if (isBorrowed) {
            sb.append("🔴 <b>QARZ OLDINGIZ (MENING QARZIM)</b>\n\n");
            sb.append("👤 <b>Kimdan / Nimadan:</b>\n").append(escapeHtml(draft.getPersonName())).append(" (shundan qarzsiz)\n\n");
            sb.append("💰 <b>Summa:</b>\n").append(MoneyFormatter.format(draft.getAmount())).append("\n\n");
            sb.append("📅 <b>Qaytarish:</b>\n")
                    .append(draft.getDueDate() != null ? draft.getDueDate().format(dFmt) : "<i>Belgilanmagan</i>").append("\n\n");
            sb.append("💳 <b>To‘lov turi:</b>\n").append(draft.getPaymentMethod() != null ? draft.getPaymentMethod() : "Naqd").append("\n\n");
            sb.append("<b>Saqlaymizmi?</b>");
        } else {
            sb.append("🤝 <b>QARZ BERISH</b>\n\n");
            sb.append("👤 <b>Kimga:</b>\n").append(escapeHtml(draft.getPersonName())).append("\n\n");
            sb.append("💰 <b>Summa:</b>\n").append(MoneyFormatter.format(draft.getAmount())).append("\n\n");
            sb.append("📅 <b>Qaytarish sanasi:</b>\n")
                    .append(draft.getDueDate() != null ? draft.getDueDate().format(dFmt) : "<i>Belgilanmagan</i>").append("\n\n");
            sb.append("💵 <b>To‘lov turi:</b>\n").append(draft.getPaymentMethod() != null ? draft.getPaymentMethod() : "Naqd").append("\n\n");
            sb.append("🤔 <b>Bu qarz qaysi foydadan berildi? Bugungimi?</b>");
        }

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

        sb.append("🔴 <b>MEN OLGAN QARZLAR (MENING QARZLARIM)</b>\n\n");
        if (borrowed.isEmpty()) {
            sb.append("<i>Olingan qarzlar yo‘q</i>\n\n");
        } else {
            int i = 1;
            for (com.hisobchi.bot.debt.entity.Debt d : borrowed) {
                sb.append(i++).append(". 👤 <b>").append(escapeHtml(d.getPersonName())).append("</b> (qarzsiz)\n");
                sb.append("💰 Qolgan: <b>").append(MoneyFormatter.format(d.getRemainingAmount())).append("</b>\n");
                if (d.getDueDate() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getDueDate());
                    String daysText = days < 0 ? "⚠️ Muddati o‘tgan!" : (days == 0 ? "⚠️ Bugun to‘lash kuni!" : days + " kun qoldi");
                    sb.append("⏰ ").append(d.getDueDate().format(shortFmt)).append(" (").append(daysText).append(")\n");
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
                sb.append(i++).append(". 👤 <b>").append(escapeHtml(d.getPersonName())).append("</b>\n");
                sb.append("💰 Qolgan: <b>").append(MoneyFormatter.format(d.getRemainingAmount())).append("</b>\n");
                if (d.getDueDate() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getDueDate());
                    String daysText = days < 0 ? "⚠️ Muddati o‘tgan!" : (days == 0 ? "🤝 Bugun qaytarish kuni!" : days + " kun qoldi");
                    sb.append("⏰ ").append(d.getDueDate().format(shortFmt)).append(" (").append(daysText).append(")\n");
                }
                sb.append("👉 /debt_view_").append(d.getId()).append("\n\n");
            }
        }

        return sb.toString();
    }

    public static String buildActiveDebtsList(List<com.hisobchi.bot.debt.entity.Debt> debts, com.hisobchi.bot.debt.entity.DebtType type) {
        StringBuilder sb = new StringBuilder();
        boolean isBorrowed = type == com.hisobchi.bot.debt.entity.DebtType.BORROWED;
        sb.append(isBorrowed ? "🔴 <b>MEN OLGAN QARZLAR (MENING QARZLARIM)</b>\n" : "🟢 <b>MEN BERGAN QARZLAR</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (debts == null || debts.isEmpty()) {
            sb.append(isBorrowed ? "<i>Olingan faol qarzlar yo‘q</i>\n" : "<i>Berilgan faol qarzlar yo‘q</i>\n");
            return sb.toString();
        }

        LocalDate today = LocalDate.now();
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        int i = 1;
        for (com.hisobchi.bot.debt.entity.Debt d : debts) {
            sb.append(i++).append(". 👤 <b>").append(escapeHtml(d.getPersonName())).append("</b>");
            if (isBorrowed) {
                sb.append(" (qarzsiz)\n");
            } else {
                sb.append("\n");
            }
            sb.append("💰 Qolgan: <b>").append(MoneyFormatter.format(d.getRemainingAmount())).append("</b>\n");
            if (d.getDueDate() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getDueDate());
                String daysText = days < 0 ? "⚠️ Muddati o‘tgan!" : (days == 0 ? "🤝 Bugun qaytarish kuni!" : days + " kun qoldi");
                sb.append("⏰ ").append(d.getDueDate().format(shortFmt)).append(" (").append(daysText).append(")\n");
            }
            sb.append("👉 /debt_view_").append(d.getId()).append("\n\n");
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
                    .append(MoneyFormatter.format(d.getOriginalAmount()))
                    .append(" (<i>").append(status).append("</i>)\n");
        }
        return sb.toString();
    }

    public static String buildDebtDetailMessage(com.hisobchi.bot.debt.entity.Debt d) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤝 <b>QARZ MA’LUMOTI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        boolean isBorrowed = d.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED;
        sb.append("👤 <b>").append(escapeHtml(d.getPersonName())).append("</b>\n\n");

        if (isBorrowed) {
            sb.append("🔴 <b>Siz qarz olgansiz (shundan qarzsiz)</b>\n\n");
        } else {
            sb.append("🟢 <b>Siz qarz bergansiz</b>\n\n");
        }

        sb.append("💰 <b>Boshlang‘ich qarz:</b>\n")
                .append(MoneyFormatter.format(d.getOriginalAmount())).append("\n\n");

        sb.append("✅ <b>To‘langan:</b>\n")
                .append(MoneyFormatter.format(d.getPaidAmount())).append("\n\n");

        sb.append(isBorrowed ? "🔴" : "🟢").append(" <b>Qolgan:</b>\n")
                .append(MoneyFormatter.format(d.getRemainingAmount())).append("\n\n");

        java.time.format.DateTimeFormatter dFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
        if (d.getDueDate() != null) {
            sb.append("📅 <b>Qaytarish:</b>\n").append(d.getDueDate().format(dFmt)).append("\n\n");
        }

        String methodLabel = isBorrowed ? "💳 <b>Olingan:</b> " : "💵 <b>Berilgan:</b> ";
        sb.append(methodLabel).append(d.getPaymentMethod() != null ? d.getPaymentMethod() : "Naqd").append("\n");

        if (d.getDescription() != null && !d.getDescription().isBlank()) {
            sb.append("\n📝 <b>Izoh:</b> <i>").append(escapeHtml(d.getDescription())).append("</i>\n");
        }

        return sb.toString();
    }

    public static String buildDebtDetailPage(com.hisobchi.bot.debt.entity.Debt d) {
        return buildDebtDetailMessage(d);
    }

    public static String buildPaymentHistoryMessage(com.hisobchi.bot.debt.entity.Debt debt, List<com.hisobchi.bot.debt.entity.DebtPayment> payments) {
        StringBuilder sb = new StringBuilder();
        boolean isBorrowed = debt.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED;
        sb.append("📜 <b>TO‘LOVLAR TARIXI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("👤 <b>").append(escapeHtml(debt.getPersonName())).append("</b>\n");
        sb.append("💰 Jami qarz: <b>").append(MoneyFormatter.format(debt.getOriginalAmount())).append("</b>\n");
        sb.append("✅ To‘langan: <b>").append(MoneyFormatter.format(debt.getPaidAmount())).append("</b>\n");
        sb.append(isBorrowed ? "🔴" : "🟢").append(" Qolgan: <b>").append(MoneyFormatter.format(debt.getRemainingAmount())).append("</b>\n\n");

        if (payments == null || payments.isEmpty()) {
            sb.append("<i>Hozircha to‘lovlar amalga oshirilmagan.</i>\n");
            return sb.toString();
        }

        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        int i = 1;
        for (com.hisobchi.bot.debt.entity.DebtPayment p : payments) {
            String date = p.getPaymentDate() != null ? p.getPaymentDate().format(dFmt) : "";
            sb.append(i++).append(". 📅 ").append(date)
                    .append(" — <b>").append(MoneyFormatter.format(p.getAmount())).append("</b>")
                    .append(" (<i>").append(p.getPaymentMethod() != null ? p.getPaymentMethod() : "Naqd").append("</i>)\n");
        }
        return sb.toString();
    }

    public static String buildDebtStatisticsMessage(com.hisobchi.bot.debt.dto.DebtStatisticsDto stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤝 <b>QARZLAR HISOBOTI</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("🔴 <b>MENING QARZLARIM</b>\n\n");
        sb.append("Boshlang‘ich:\n<b>").append(MoneyFormatter.format(stats.borrowedOriginal())).append("</b>\n\n");
        sb.append("To‘langan:\n<b>").append(MoneyFormatter.format(stats.borrowedPaid())).append("</b>\n\n");
        sb.append("Qolgan:\n<b>").append(MoneyFormatter.format(stats.borrowedRemaining())).append("</b>\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("🟢 <b>MEN BERGAN QARZLAR</b>\n\n");
        sb.append("Berilgan:\n<b>").append(MoneyFormatter.format(stats.lentOriginal())).append("</b>\n\n");
        sb.append("Qaytgan:\n<b>").append(MoneyFormatter.format(stats.lentPaid())).append("</b>\n\n");
        sb.append("Boshqalarda qolgan:\n<b>").append(MoneyFormatter.format(stats.lentRemaining())).append("</b>\n");

        return sb.toString();
    }

    public static String buildDebtPaymentHistoryMessage(com.hisobchi.bot.debt.entity.Debt debt, List<com.hisobchi.bot.debt.entity.DebtPayment> payments) {
        StringBuilder sb = new StringBuilder();
        boolean isBorrowed = debt.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED;
        String title = isBorrowed ? "TO‘LOVLAR TARIXI" : "QAYTARISHLAR TARIXI";
        sb.append("📜 <b>").append(title).append("</b>\n");
        sb.append("👤 <b>").append(escapeHtml(debt.getPersonName())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (payments == null || payments.isEmpty()) {
            sb.append("<i>To‘lovlar tarixi mavjud emas.</i>\n\n");
        } else {
            java.time.format.DateTimeFormatter dFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
            int i = 1;
            for (com.hisobchi.bot.debt.entity.DebtPayment p : payments) {
                sb.append(i++).append(". <b>").append(MoneyFormatter.format(p.getAmount())).append("</b> ")
                        .append("(").append(p.getPaymentMethod()).append(") — ")
                        .append(p.getPaymentDate() != null ? p.getPaymentDate().format(dFmt) : "").append("\n");
            }
            sb.append("\n");
        }

        sb.append("💰 Boshlang‘ich: <b>").append(MoneyFormatter.format(debt.getOriginalAmount())).append("</b>\n");
        sb.append("✅ Jami to‘langan: <b>").append(MoneyFormatter.format(debt.getPaidAmount())).append("</b>\n");
        sb.append(isBorrowed ? "🔴" : "🟢").append(" Qolgan qarz: <b>").append(MoneyFormatter.format(debt.getRemainingAmount())).append("</b>\n");
        return sb.toString();
    }

    public static String buildNearDueDebtsMessage(List<com.hisobchi.bot.debt.entity.Debt> debts) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ <b>MUDDATI YAQIN QARZLAR</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");

        if (debts == null || debts.isEmpty()) {
            sb.append("<i>Yaqin kunlarda muddati keladigan qarzlar yo‘q.</i>\n");
            return sb.toString();
        }

        LocalDate today = LocalDate.now();
        java.time.format.DateTimeFormatter shortFmt = java.time.format.DateTimeFormatter.ofPattern("d-MMMM", java.util.Locale.forLanguageTag("uz-UZ"));

        int i = 1;
        for (com.hisobchi.bot.debt.entity.Debt d : debts) {
            String icon = d.getType() == com.hisobchi.bot.debt.entity.DebtType.BORROWED ? "🔴" : "🟢";
            sb.append(icon).append(" ").append(i++).append(". <b>").append(escapeHtml(d.getPersonName())).append("</b>\n");
            sb.append("💰 Qolgan: <b>").append(MoneyFormatter.format(d.getRemainingAmount())).append("</b>\n");
            if (d.getDueDate() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getDueDate());
                String daysText = days < 0 ? "⚠️ Muddati o‘tgan!" : (days == 0 ? "⚠️ Bugun qaytarish kuni!" : days + " kun qoldi");
                sb.append("⏰ ").append(d.getDueDate().format(shortFmt)).append(" (").append(daysText).append(")\n");
            }
            sb.append("👉 /debt_view_").append(d.getId()).append("\n\n");
        }
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
