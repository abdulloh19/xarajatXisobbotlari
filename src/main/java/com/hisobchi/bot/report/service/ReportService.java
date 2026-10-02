package com.hisobchi.bot.report.service;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.report.dto.ReportData;
import com.hisobchi.bot.statistics.dto.CategoryExpenseDto;
import com.hisobchi.bot.telegram.handler.BotMessageBuilder;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final DailyProfitService dailyProfitService;
    private final com.hisobchi.bot.debt.repository.DebtRepository debtRepository;
    private final com.hisobchi.bot.debt.repository.DebtPaymentRepository debtPaymentRepository;
    private final com.hisobchi.bot.user.service.BalanceService balanceService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d-MMMM yyyy", new Locale("uz"));
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("d-MMMM", new Locale("uz"));

    @Transactional(readOnly = true)
    public ReportData getDailyReportData(Long userId, LocalDate date) {
        BigDecimal expense = transactionRepository.sumAmountByUserIdAndTypeAndDate(userId, TransactionType.EXPENSE, date);
        if (expense == null) expense = BigDecimal.ZERO;

        Optional<DailyProfit> profitOpt = dailyProfitService.getProfit(userId, date);

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        boolean entered = false;

        if (profitOpt.isPresent()) {
            DailyProfit p = profitOpt.get();
            cash = p.getCashAmount();
            card = p.getCardAmount();
            profit = p.getTotalProfit();
            entered = true;
        }

        // Section 64: JAMI ISHLANGAN = XARAJAT + USER KIRITGAN FOYDA
        BigDecimal totalEarned = expense.add(profit);
        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesByDate(userId, TransactionType.EXPENSE, date);

        return ReportData.builder()
                .title("BUGUNGI YAKUNIY HISOBOT")
                .periodStart(date)
                .periodEnd(date)
                .totalEarned(totalEarned)
                .totalExpense(expense)
                .totalProfit(profit)
                .cashProfit(cash)
                .cardProfit(card)
                .activeDays(1)
                .completedDays(entered ? 1 : 0)
                .incompleteDays(entered ? 0 : 1)
                .incompleteDates(entered ? List.of() : List.of(date))
                .categoryExpenses(categories)
                .profitEntered(entered)
                .build();
    }

    @Transactional(readOnly = true)
    public ReportData getPeriodReportData(Long userId, LocalDate start, LocalDate end, String title) {
        BigDecimal expense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, start, end);
        if (expense == null) expense = BigDecimal.ZERO;

        List<DailyProfit> profitList = dailyProfitService.getProfitsBetween(userId, start, end);
        Map<LocalDate, DailyProfit> profitMap = new HashMap<>();
        for (DailyProfit dp : profitList) {
            profitMap.put(dp.getProfitDate(), dp);
        }

        BigDecimal cashSum = BigDecimal.ZERO;
        BigDecimal cardSum = BigDecimal.ZERO;
        BigDecimal profitSum = BigDecimal.ZERO;

        long completedDays = 0;
        long incompleteDays = 0;
        List<LocalDate> incompleteDates = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DailyProfit p = profitMap.get(d);
            BigDecimal dayExp = transactionRepository.sumAmountByUserIdAndTypeAndDate(userId, TransactionType.EXPENSE, d);
            boolean hasExp = dayExp != null && dayExp.compareTo(BigDecimal.ZERO) > 0;

            if (p != null && p.getTotalProfit().compareTo(BigDecimal.ZERO) > 0) {
                completedDays++;
                cashSum = cashSum.add(p.getCashAmount());
                cardSum = cardSum.add(p.getCardAmount());
                profitSum = profitSum.add(p.getTotalProfit());
            } else if (hasExp) {
                incompleteDays++;
                incompleteDates.add(d);
            }
        }

        // Section 64: totalEarned = SUM(each day's expense + each day's profit)
        BigDecimal totalEarned = expense.add(profitSum);

        long activeDays = transactionRepository.countActiveDaysBetween(userId, start, end);
        if (activeDays == 0 && completedDays > 0) {
            activeDays = completedDays;
        }

        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesBetween(
                userId, TransactionType.EXPENSE, start, end);

        return ReportData.builder()
                .title(title)
                .periodStart(start)
                .periodEnd(end)
                .totalEarned(totalEarned)
                .totalExpense(expense)
                .totalProfit(profitSum)
                .cashProfit(cashSum)
                .cardProfit(cardSum)
                .activeDays(Math.max(activeDays, 1))
                .completedDays(completedDays)
                .incompleteDays(incompleteDays)
                .incompleteDates(incompleteDates)
                .categoryExpenses(categories)
                .profitEntered(completedDays > 0)
                .build();
    }

    public String formatDailyReport(ReportData data) {
        if (!data.profitEntered()) {
            return String.format(
                    "⚠️ <b>Bugungi foyda hali kiritilmagan.</b>\n\n" +
                    "💸 <b>Bugungi xarajat:</b>\n%s\n\n" +
                    "💰 Jami ishlangan pulni hisoblash uchun foydani kiriting.",
                    MoneyFormatter.format(data.totalExpense())
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>BUGUNGI HISOBOT</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("📅 <b>").append(data.periodStart().format(DATE_FMT)).append("</b>\n\n");
        sb.append("💰 <b>Umumiy ishlab topilgan:</b>\n").append(MoneyFormatter.format(data.totalEarned())).append("\n\n");
        sb.append("💸 <b>Xarajatlar:</b>\n").append(MoneyFormatter.format(data.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>BUGUNGI FOYDANGIZ:</b>\n<b>").append(MoneyFormatter.format(data.totalProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        if (data.cashProfit().compareTo(BigDecimal.ZERO) > 0 || data.cardProfit().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💵 <b>Naqd:</b> ").append(MoneyFormatter.format(data.cashProfit())).append("\n");
            sb.append("💳 <b>Karta:</b> ").append(MoneyFormatter.format(data.cardProfit())).append("\n\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n");
        }
        sb.append("📁 <b>XARAJATLAR:</b>\n\n");

        if (data.categoryExpenses().isEmpty()) {
            sb.append("<i>Xarajatlar mavjud emas</i>\n");
        } else {
            for (CategoryExpenseDto c : data.categoryExpenses()) {
                sb.append(c.categoryEmoji()).append(" ").append(BotMessageBuilder.escapeHtml(c.categoryName()))
                        .append("\n").append(MoneyFormatter.format(c.totalAmount())).append("\n\n");
            }
        }

        return sb.toString();
    }

    public String formatDailyReport(ReportData data, Long userId) {
        StringBuilder sb = new StringBuilder();
        if (!data.profitEntered()) {
            sb.append(String.format(
                    "⚠️ <b>Bugungi foyda hali kiritilmagan.</b>\n\n" +
                    "💸 <b>Bugungi xarajatlar:</b>\n%s\n\n" +
                    "💰 Jami ishlangan pulni hisoblash uchun foydani kiriting.\n\n",
                    MoneyFormatter.format(data.totalExpense())
            ));
        } else {
            sb.append("📊 <b>BUGUNGI HISOBOT</b>\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n\n");
            sb.append("📅 <b>").append(data.periodStart().format(DATE_FMT)).append("</b>\n\n");
            sb.append("💰 <b>Umumiy ishlab topilgan:</b>\n").append(MoneyFormatter.format(data.totalEarned())).append("\n\n");
            sb.append("💸 <b>Xarajatlar:</b>\n").append(MoneyFormatter.format(data.totalExpense())).append("\n\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("✅ <b>BUGUNGI FOYDANGIZ:</b>\n<b>").append(MoneyFormatter.format(data.totalProfit())).append("</b>\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n\n");
            if (data.cashProfit().compareTo(BigDecimal.ZERO) > 0 || data.cardProfit().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("💵 <b>Naqd:</b> ").append(MoneyFormatter.format(data.cashProfit())).append("\n");
                sb.append("💳 <b>Karta:</b> ").append(MoneyFormatter.format(data.cardProfit())).append("\n\n");
            }
        }

        // Debt movements for today (Section 28)
        LocalDate date = data.periodStart();
        BigDecimal lentCreated = debtRepository.sumCreatedAmountByUserIdAndTypeAndDate(userId, com.hisobchi.bot.debt.entity.DebtType.LENT, date);
        BigDecimal borrowedCreated = debtRepository.sumCreatedAmountByUserIdAndTypeAndDate(userId, com.hisobchi.bot.debt.entity.DebtType.BORROWED, date);
        BigDecimal returnedReceived = debtPaymentRepository.sumAmountByUserIdAndPaymentTypeAndPaymentDate(
                userId, com.hisobchi.bot.debt.entity.DebtPaymentType.DEBT_RETURN, date);
        BigDecimal debtPaid = debtPaymentRepository.sumAmountByUserIdAndPaymentTypeAndPaymentDate(
                userId, com.hisobchi.bot.debt.entity.DebtPaymentType.DEBT_PAYMENT, date);

        boolean hasDebtMovements = (lentCreated != null && lentCreated.compareTo(BigDecimal.ZERO) > 0)
                || (borrowedCreated != null && borrowedCreated.compareTo(BigDecimal.ZERO) > 0)
                || (returnedReceived != null && returnedReceived.compareTo(BigDecimal.ZERO) > 0)
                || (debtPaid != null && debtPaid.compareTo(BigDecimal.ZERO) > 0);

        if (hasDebtMovements) {
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("🤝 <b>QARZ HARAKATLARI</b>\n\n");
            if (lentCreated != null && lentCreated.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("💸 <b>Qarzga berdingiz:</b>\n").append(MoneyFormatter.format(lentCreated)).append("\n\n");
            }
            if (borrowedCreated != null && borrowedCreated.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("💰 <b>Qarz oldingiz:</b>\n").append(MoneyFormatter.format(borrowedCreated)).append("\n\n");
            }
            if (returnedReceived != null && returnedReceived.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("💵 <b>Qarzdan qaytdi:</b>\n").append(MoneyFormatter.format(returnedReceived)).append("\n\n");
            }
            if (debtPaid != null && debtPaid.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("🔴 <b>Qarz to‘ladingiz:</b>\n").append(MoneyFormatter.format(debtPaid)).append("\n\n");
            }
        }

        // Available real balance
        var balanceOpt = balanceService.getBalance(userId);
        if (balanceOpt.isPresent()) {
            var b = balanceOpt.get();
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("💵 <b>Hozir naqd:</b> ").append(MoneyFormatter.format(b.getCashBalance())).append("\n");
            sb.append("💳 <b>Hozir kartada:</b> ").append(MoneyFormatter.format(b.getCardBalance())).append("\n");
            sb.append("💰 <b>Jami mavjud:</b> ").append(MoneyFormatter.format(b.getAvailableBalance())).append("\n\n");
        }

        if (!data.categoryExpenses().isEmpty()) {
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("📂 <b>Xarajatlar:</b>\n\n");
            for (CategoryExpenseDto c : data.categoryExpenses()) {
                sb.append(c.categoryEmoji()).append(" ").append(BotMessageBuilder.escapeHtml(c.categoryName()))
                        .append(" — <b>").append(MoneyFormatter.format(c.totalAmount())).append("</b>\n");
            }
        }

        return sb.toString();
    }

    public String formatPeriodReport(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>").append(data.title()).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("📅 <b>").append(data.periodStart().format(SHORT_DATE_FMT))
                .append(" — ").append(data.periodEnd().format(DATE_FMT)).append("</b>\n\n");

        sb.append("💰 <b>Umumiy ishlab topilgan:</b>\n").append(MoneyFormatter.format(data.totalEarned())).append("\n\n");
        sb.append("💸 <b>Xarajatlar:</b>\n").append(MoneyFormatter.format(data.totalExpense())).append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ <b>FOYDANGIZ:</b>\n<b>").append(MoneyFormatter.format(data.totalProfit())).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        if (data.cashProfit().compareTo(BigDecimal.ZERO) > 0 || data.cardProfit().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("💵 <b>Naqd:</b> ").append(MoneyFormatter.format(data.cashProfit())).append("\n");
            sb.append("💳 <b>Karta:</b> ").append(MoneyFormatter.format(data.cardProfit())).append("\n\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n");
        }
        sb.append("📁 <b>XARAJATLAR:</b>\n\n");

        if (data.categoryExpenses().isEmpty()) {
            sb.append("<i>Xarajatlar mavjud emas</i>\n");
        } else {
            for (CategoryExpenseDto c : data.categoryExpenses()) {
                sb.append(c.categoryEmoji()).append(" ").append(BotMessageBuilder.escapeHtml(c.categoryName()))
                        .append(" — <b>").append(MoneyFormatter.format(c.totalAmount())).append("</b>\n");
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━\n");
        sb.append("📆 <b>Ishlangan kunlar:</b> ").append(data.activeDays()).append(" kun\n\n");

        long daysDiv = Math.max(data.activeDays(), 1);
        BigDecimal avgEarned = data.totalEarned().divide(BigDecimal.valueOf(daysDiv), 0, RoundingMode.HALF_UP);
        BigDecimal avgProfit = data.totalProfit().divide(BigDecimal.valueOf(daysDiv), 0, RoundingMode.HALF_UP);

        sb.append("💰 <b>O‘rtacha kunlik ishlab topilgan:</b> ").append(MoneyFormatter.format(avgEarned)).append("\n");
        sb.append("✅ <b>O‘rtacha kunlik foyda:</b> ").append(MoneyFormatter.format(avgProfit)).append("\n");

        if (data.incompleteDays() > 0) {
            sb.append("\n⚠️ <b>Foydasi kiritilmagan kunlar:</b> ").append(data.incompleteDays()).append(" kun\n");
            for (LocalDate d : data.incompleteDates()) {
                sb.append("📅 ").append(d.format(SHORT_DATE_FMT)).append("\n");
            }
        }

        return sb.toString();
    }
}
