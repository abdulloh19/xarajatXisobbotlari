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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final TransactionRepository transactionRepository;
    private final DailySummaryService dailySummaryService;

    @Transactional(readOnly = true)
    public DailyStatisticsDto getDailyStatistics(User user, LocalDate date) {
        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, date);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.EXPENSE, date);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        long count = transactionRepository.countByUserIdAndDate(user.getId(), date);
        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesByDate(user.getId(), TransactionType.EXPENSE, date);
        boolean isClosed = dailySummaryService.isDayClosed(user.getId(), date);

        return new DailyStatisticsDto(date, totalIncome, totalExpense, netProfit, count, categories, isClosed);
    }

    @Transactional(readOnly = true)
    public WeeklyStatisticsDto getWeeklyStatistics(User user, LocalDate referenceDate) {
        LocalDate startDate = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        long count = transactionRepository.countByUserIdAndDateBetween(user.getId(), startDate, endDate);
        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);

        CategoryExpenseDto topCategory = categories.isEmpty() ? null : categories.get(0);

        return new WeeklyStatisticsDto(startDate, endDate, totalIncome, totalExpense, netProfit, count, categories, topCategory);
    }

    @Transactional(readOnly = true)
    public MonthlyStatisticsDto getMonthlyStatistics(User user, LocalDate referenceDate) {
        LocalDate startDate = referenceDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endDate = referenceDate.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        long activeDays = transactionRepository.countActiveDaysBetween(user.getId(), startDate, endDate);
        if (activeDays == 0) {
            activeDays = 1;
        }

        BigDecimal activeDaysBd = BigDecimal.valueOf(activeDays);
        BigDecimal avgDailyIncome = totalIncome.divide(activeDaysBd, 0, RoundingMode.HALF_UP);
        BigDecimal avgDailyExpense = totalExpense.divide(activeDaysBd, 0, RoundingMode.HALF_UP);
        BigDecimal avgDailyNetProfit = netProfit.divide(activeDaysBd, 0, RoundingMode.HALF_UP);

        List<CategoryExpenseDto> categories = transactionRepository.findCategoryExpensesBetween(
                user.getId(), TransactionType.EXPENSE, startDate, endDate);

        return new MonthlyStatisticsDto(
                referenceDate,
                totalIncome,
                totalExpense,
                netProfit,
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
