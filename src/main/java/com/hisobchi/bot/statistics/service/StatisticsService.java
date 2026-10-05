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

    @Transactional(readOnly = true)
    public DailyStatisticsDto getDailyStatistics(User user, LocalDate date) {
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.EXPENSE, date);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        Optional<DailyProfit> profitOpt = dailyProfitService.getProfit(user.getId(), date);
        BigDecimal enteredProfit = BigDecimal.ZERO;
        if (profitOpt.isPresent()) {
            enteredProfit = profitOpt.get().getTotalProfit();
        } else {
            BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, date);
            if (inc != null) enteredProfit = inc;
        }

        // FORMULA: totalEarned = profit + expenses, netProfit = enteredProfit (real profit)
        BigDecimal totalEarned = enteredProfit.add(totalExpense);
        BigDecimal netProfit = enteredProfit;

        long count = transactionRepository.countByUserIdAndDate(user.getId(), date);
        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesByDate(user.getId(), TransactionType.EXPENSE, date);
        boolean isClosed = dailySummaryService.isDayClosed(user.getId(), date);
        boolean isOffDay = dailyProfitService.isOffDay(user.getId(), date);

        return new DailyStatisticsDto(date, totalEarned, totalExpense, netProfit, count, categories, isClosed, isOffDay);
    }

    @Transactional(readOnly = true)
    public WeeklyStatisticsDto getWeeklyStatistics(User user, LocalDate referenceDate) {
        LocalDate startDate = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

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

        BigDecimal totalEarned = totalProfit.add(totalExpense);
        BigDecimal netProfit = totalProfit;
        long count = transactionRepository.countByUserIdAndDateBetween(user.getId(), startDate, endDate);
        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);

        CategoryExpenseDto topCategory = categories.isEmpty() ? null : categories.get(0);

        return new WeeklyStatisticsDto(startDate, endDate, totalEarned, totalExpense, netProfit, count, categories, topCategory);
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

        BigDecimal totalEarned = totalProfit.add(totalExpense);
        BigDecimal netProfit = totalProfit;

        long txActiveDays = transactionRepository.countActiveDaysBetween(user.getId(), startDate, endDate);
        long activeDays = Math.max(txActiveDays, completedDays);
        if (activeDays == 0) {
            activeDays = 1;
        }

        BigDecimal activeDaysBd = BigDecimal.valueOf(activeDays);
        BigDecimal avgDailyIncome = totalEarned.divide(activeDaysBd, 0, RoundingMode.HALF_UP);
        BigDecimal avgDailyExpense = totalExpense.divide(activeDaysBd, 0, RoundingMode.HALF_UP);
        BigDecimal avgDailyNetProfit = netProfit.divide(activeDaysBd, 0, RoundingMode.HALF_UP);

        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);

        return new MonthlyStatisticsDto(
                referenceDate,
                totalEarned,
                totalExpense,
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
