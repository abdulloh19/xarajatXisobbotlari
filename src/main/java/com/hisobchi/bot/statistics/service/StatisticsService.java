package com.hisobchi.bot.statistics.service;

import com.hisobchi.bot.statistics.dto.CategoryExpenseDto;
import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.dto.MonthlyStatisticsDto;
import com.hisobchi.bot.statistics.dto.WeeklyStatisticsDto;
import com.hisobchi.bot.summary.service.DailySummaryService;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.debt.entity.DebtPaymentType;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtPaymentRepository;
import com.hisobchi.bot.debt.repository.DebtRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final TransactionRepository transactionRepository;
    private final DailySummaryService dailySummaryService;
    private final DailyProfitService dailyProfitService;
    private final DebtRepository debtRepository;
    private final DebtPaymentRepository debtPaymentRepository;

    @Transactional(readOnly = true)
    public DailyStatisticsDto getDailyStatistics(User user, LocalDate date) {
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.EXPENSE, date);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal lentDebt = debtRepository != null
                ? debtRepository.sumCreatedAmountByUserIdAndTypeAndDate(user.getId(), DebtType.LENT, date)
                : BigDecimal.ZERO;
        if (lentDebt == null) lentDebt = BigDecimal.ZERO;

        BigDecimal paidDebt = debtPaymentRepository != null
                ? debtPaymentRepository.sumAmountByUserIdAndPaymentTypeAndPaymentDate(user.getId(), DebtPaymentType.DEBT_PAYMENT, date)
                : BigDecimal.ZERO;
        if (paidDebt == null) paidDebt = BigDecimal.ZERO;

        BigDecimal totalExpenseWithDebts = totalExpense.add(lentDebt).add(paidDebt);

        Optional<DailyProfit> profitOpt = dailyProfitService.getProfit(user.getId(), date);
        BigDecimal enteredProfit = BigDecimal.ZERO;
        if (profitOpt.isPresent()) {
            enteredProfit = profitOpt.get().getTotalProfit();
        } else {
            BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, date);
            if (inc != null) enteredProfit = inc;
        }

        // FORMULA: totalEarned = profit + expenses, netProfit = enteredProfit (real profit)
        BigDecimal totalEarned = enteredProfit.add(totalExpenseWithDebts);
        BigDecimal netProfit = enteredProfit;

        long lentCount = (lentDebt.compareTo(BigDecimal.ZERO) > 0 && debtRepository != null)
                ? debtRepository.countCreatedByUserIdAndTypeAndDate(user.getId(), DebtType.LENT, date)
                : 0L;
        long paidDebtCount = (paidDebt.compareTo(BigDecimal.ZERO) > 0 && debtPaymentRepository != null)
                ? debtPaymentRepository.countByUserIdAndPaymentTypeAndPaymentDate(user.getId(), DebtPaymentType.DEBT_PAYMENT, date)
                : 0L;
        long count = transactionRepository.countByUserIdAndDate(user.getId(), date) + lentCount + paidDebtCount;

        List<CategoryExpenseDto> categories = new ArrayList<>(
                transactionRepository.findCategoryExpensesByDate(user.getId(), TransactionType.EXPENSE, date)
        );
        if (lentDebt.compareTo(BigDecimal.ZERO) > 0) {
            categories.add(new CategoryExpenseDto(null, "Berilgan qarz", "🤝", lentDebt, Math.max(1, lentCount)));
        }
        if (paidDebt.compareTo(BigDecimal.ZERO) > 0) {
            categories.add(new CategoryExpenseDto(null, "To‘langan qarz", "💳", paidDebt, Math.max(1, paidDebtCount)));
        }
        categories.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        boolean isClosed = dailySummaryService.isDayClosed(user.getId(), date);
        boolean isOffDay = dailyProfitService.isOffDay(user.getId(), date);

        return new DailyStatisticsDto(date, totalEarned, totalExpenseWithDebts, netProfit, count, categories, isClosed, isOffDay);
    }

    @Transactional(readOnly = true)
    public WeeklyStatisticsDto getWeeklyStatistics(User user, LocalDate referenceDate) {
        LocalDate startDate = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal lentDebt = debtRepository != null
                ? debtRepository.sumCreatedAmountByUserIdAndTypeAndDateBetween(user.getId(), DebtType.LENT, startDate, endDate)
                : BigDecimal.ZERO;
        if (lentDebt == null) lentDebt = BigDecimal.ZERO;

        BigDecimal paidDebt = debtPaymentRepository != null
                ? debtPaymentRepository.sumAmountByUserIdAndPaymentTypeAndPaymentDateBetween(user.getId(), DebtPaymentType.DEBT_PAYMENT, startDate, endDate)
                : BigDecimal.ZERO;
        if (paidDebt == null) paidDebt = BigDecimal.ZERO;

        BigDecimal totalExpenseWithDebts = totalExpense.add(lentDebt).add(paidDebt);

        List<DailyProfit> profitList = dailyProfitService.getProfitsBetween(user.getId(), startDate, endDate);
        Map<LocalDate, DailyProfit> profitMap = new HashMap<>();
        for (DailyProfit dp : profitList) {
            profitMap.put(dp.getProfitDate(), dp);
        }

        BigDecimal totalProfit = BigDecimal.ZERO;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            DailyProfit p = profitMap.get(d);
            if (p != null && !p.isWorkDay()) {
                continue;
            }
            if (p != null && p.isWorkDay()) {
                if (p.getTotalProfit() != null) totalProfit = totalProfit.add(p.getTotalProfit());
            } else {
                BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, d);
                if (inc != null && inc.compareTo(BigDecimal.ZERO) > 0) {
                    totalProfit = totalProfit.add(inc);
                }
            }
        }

        BigDecimal totalEarned = totalProfit.add(totalExpenseWithDebts);
        BigDecimal netProfit = totalProfit;

        long lentCount = (lentDebt.compareTo(BigDecimal.ZERO) > 0 && debtRepository != null)
                ? debtRepository.countCreatedByUserIdAndTypeAndDateBetween(user.getId(), DebtType.LENT, startDate, endDate)
                : 0L;
        long paidDebtCount = (paidDebt.compareTo(BigDecimal.ZERO) > 0 && debtPaymentRepository != null)
                ? debtPaymentRepository.countByUserIdAndPaymentTypeAndPaymentDateBetween(user.getId(), DebtPaymentType.DEBT_PAYMENT, startDate, endDate)
                : 0L;
        long count = transactionRepository.countByUserIdAndDateBetween(user.getId(), startDate, endDate) + lentCount + paidDebtCount;

        List<CategoryExpenseDto> categories = new ArrayList<>(
                transactionRepository.findCategoryExpensesBetween(
                        user.getId(), TransactionType.EXPENSE, startDate, endDate)
        );
        if (lentDebt.compareTo(BigDecimal.ZERO) > 0) {
            categories.add(new CategoryExpenseDto(null, "Berilgan qarz", "🤝", lentDebt, Math.max(1, lentCount)));
        }
        if (paidDebt.compareTo(BigDecimal.ZERO) > 0) {
            categories.add(new CategoryExpenseDto(null, "To‘langan qarz", "💳", paidDebt, Math.max(1, paidDebtCount)));
        }
        categories.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        CategoryExpenseDto topCategory = categories.isEmpty() ? null : categories.get(0);

        return new WeeklyStatisticsDto(startDate, endDate, totalEarned, totalExpenseWithDebts, netProfit, count, categories, topCategory);
    }

    @Transactional(readOnly = true)
    public MonthlyStatisticsDto getMonthlyStatistics(User user, LocalDate referenceDate) {
        LocalDate startDate = referenceDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate today = user.getTimezone() != null ? DateTimeUtils.today(user.getTimezone()) : LocalDate.now();
        boolean isCurrentMonth = referenceDate.getYear() == today.getYear() && referenceDate.getMonth() == today.getMonth();
        LocalDate endDate = (isCurrentMonth && today.isBefore(referenceDate.with(TemporalAdjusters.lastDayOfMonth())))
                ? today
                : referenceDate.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal lentDebt = debtRepository != null
                ? debtRepository.sumCreatedAmountByUserIdAndTypeAndDateBetween(user.getId(), DebtType.LENT, startDate, endDate)
                : BigDecimal.ZERO;
        if (lentDebt == null) lentDebt = BigDecimal.ZERO;

        BigDecimal paidDebt = debtPaymentRepository != null
                ? debtPaymentRepository.sumAmountByUserIdAndPaymentTypeAndPaymentDateBetween(user.getId(), DebtPaymentType.DEBT_PAYMENT, startDate, endDate)
                : BigDecimal.ZERO;
        if (paidDebt == null) paidDebt = BigDecimal.ZERO;

        BigDecimal totalExpenseWithDebts = totalExpense.add(lentDebt).add(paidDebt);

        List<DailyProfit> profitList = dailyProfitService.getProfitsBetween(user.getId(), startDate, endDate);
        Map<LocalDate, DailyProfit> profitMap = new HashMap<>();
        for (DailyProfit dp : profitList) {
            profitMap.put(dp.getProfitDate(), dp);
        }

        BigDecimal cashSum = BigDecimal.ZERO;
        BigDecimal cardSum = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        long completedDays = 0;

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            DailyProfit p = profitMap.get(d);
            if (p != null && !p.isWorkDay()) {
                continue;
            }
            if (p != null && p.isWorkDay()) {
                completedDays++;
                if (p.getCashAmount() != null) cashSum = cashSum.add(p.getCashAmount());
                if (p.getCardAmount() != null) cardSum = cardSum.add(p.getCardAmount());
                if (p.getTotalProfit() != null) totalProfit = totalProfit.add(p.getTotalProfit());
            } else {
                BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, d);
                if (inc != null && inc.compareTo(BigDecimal.ZERO) > 0) {
                    completedDays++;
                    totalProfit = totalProfit.add(inc);
                }
            }
        }

        BigDecimal totalEarned = totalProfit.add(totalExpenseWithDebts);
        BigDecimal netProfit = totalProfit;

        long txActiveDays = transactionRepository.countActiveDaysBetween(user.getId(), startDate, endDate);
        long activeDays = Math.max(txActiveDays, completedDays);
        if (activeDays == 0) {
            activeDays = 1;
        }

        BigDecimal activeDaysBd = BigDecimal.valueOf(activeDays);
        BigDecimal avgDailyIncome = totalEarned.divide(activeDaysBd, 0, RoundingMode.HALF_UP);
        BigDecimal avgDailyExpense = totalExpenseWithDebts.divide(activeDaysBd, 0, RoundingMode.HALF_UP);
        BigDecimal avgDailyNetProfit = netProfit.divide(activeDaysBd, 0, RoundingMode.HALF_UP);

        List<CategoryExpenseDto> categories = new ArrayList<>(
                transactionRepository.findCategoryExpensesBetween(
                        user.getId(), TransactionType.EXPENSE, startDate, endDate)
        );
        if (lentDebt.compareTo(BigDecimal.ZERO) > 0) {
            long lentCount = debtRepository != null
                    ? debtRepository.countCreatedByUserIdAndTypeAndDateBetween(user.getId(), DebtType.LENT, startDate, endDate)
                    : 1L;
            categories.add(new CategoryExpenseDto(null, "Berilgan qarz", "🤝", lentDebt, Math.max(1, lentCount)));
        }
        if (paidDebt.compareTo(BigDecimal.ZERO) > 0) {
            long paidCount = debtPaymentRepository != null
                    ? debtPaymentRepository.countByUserIdAndPaymentTypeAndPaymentDateBetween(user.getId(), DebtPaymentType.DEBT_PAYMENT, startDate, endDate)
                    : 1L;
            categories.add(new CategoryExpenseDto(null, "To‘langan qarz", "💳", paidDebt, Math.max(1, paidCount)));
        }
        categories.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        return new MonthlyStatisticsDto(
                referenceDate,
                totalEarned,
                totalExpenseWithDebts,
                netProfit,
                cashSum,
                cardSum,
                activeDays,
                avgDailyIncome,
                avgDailyExpense,
                avgDailyNetProfit,
                categories
        );
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsForDate(User user, LocalDate date) {
        return transactionRepository.findByUserIdAndTransactionDateOrderByCreatedAtAsc(user.getId(), date)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getHistoryBetween(User user, LocalDate start, LocalDate end, int page, int size) {
        return transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                user.getId(), start, end, PageRequest.of(page, size)
        ).map(this::toDto);
    }

    public TransactionDto toDto(Transaction t) {
        String catName = t.getCategory() != null ? t.getCategory().getName() : "Boshqa";
        String catEmoji = t.getCategory() != null ? t.getCategory().getEmoji() : "📌";
        Long catId = t.getCategory() != null ? t.getCategory().getId() : null;

        return new TransactionDto(
                t.getId(),
                t.getUser().getId(),
                catId,
                catName,
                catEmoji,
                t.getType(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription(),
                t.getSource(),
                t.getTransactionDate(),
                t.getCreatedAt()
        );
    }
}
