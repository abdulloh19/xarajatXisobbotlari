package com.hisobchi.bot.statistics;

import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.report.dto.ReportData;
import com.hisobchi.bot.report.service.ReportService;
import com.hisobchi.bot.statistics.dto.MonthlyStatisticsDto;
import com.hisobchi.bot.statistics.service.StatisticsService;
import com.hisobchi.bot.summary.service.DailySummaryService;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsReportConsistencyTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private DailyProfitService dailyProfitService;
    @Mock
    private DailySummaryService dailySummaryService;
    @Mock
    private com.hisobchi.bot.debt.repository.DebtRepository debtRepository;
    @Mock
    private com.hisobchi.bot.debt.repository.DebtPaymentRepository debtPaymentRepository;
    @Mock
    private com.hisobchi.bot.user.service.BalanceService balanceService;

    @InjectMocks
    private StatisticsService statisticsService;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("Monthly statistics and period report data should match on totalEarned, totalExpense, and netProfit")
    void testMonthlyStatisticsAndReportConsistency() {
        User user = User.builder().id(1L).timezone("Asia/Tashkent").build();
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 5);

        BigDecimal totalExpense = new BigDecimal("450000");
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(eq(user.getId()), eq(TransactionType.EXPENSE), eq(start), any(LocalDate.class)))
                .thenReturn(totalExpense);

        List<DailyProfit> profits = List.of(
                DailyProfit.builder().profitDate(start).totalProfit(new BigDecimal("300000")).cashAmount(new BigDecimal("200000")).cardAmount(new BigDecimal("100000")).isWorkDay(true).build(),
                DailyProfit.builder().profitDate(start.plusDays(1)).totalProfit(new BigDecimal("400000")).cashAmount(new BigDecimal("250000")).cardAmount(new BigDecimal("150000")).isWorkDay(true).build(),
                DailyProfit.builder().profitDate(start.plusDays(2)).totalProfit(BigDecimal.ZERO).isWorkDay(false).build(), // off day
                DailyProfit.builder().profitDate(start.plusDays(3)).totalProfit(new BigDecimal("500000")).cashAmount(new BigDecimal("500000")).cardAmount(BigDecimal.ZERO).isWorkDay(true).build()
        );

        when(dailyProfitService.getProfitsBetween(eq(user.getId()), eq(start), any(LocalDate.class)))
                .thenReturn(profits);

        when(transactionRepository.countActiveDaysBetween(eq(user.getId()), eq(start), any(LocalDate.class)))
                .thenReturn(3L);

        when(transactionRepository.findCategoryExpensesBetween(eq(user.getId()), eq(TransactionType.EXPENSE), eq(start), any(LocalDate.class)))
                .thenReturn(List.of());

        ReportData reportData = reportService.getPeriodReportData(user.getId(), start, end, "Oktyabr 2026 HISOBOTI");
        MonthlyStatisticsDto stats = statisticsService.getMonthlyStatistics(user, end);

        // Verify total profit matches: 300k + 400k + 500k = 1 200 000
        assertEquals(new BigDecimal("1200000"), reportData.totalProfit());
        assertEquals(new BigDecimal("1200000"), stats.netProfit());

        // Verify total expense matches: 450 000
        assertEquals(new BigDecimal("450000"), reportData.totalExpense());
        assertEquals(new BigDecimal("450000"), stats.totalExpense());

        // Verify total earned matches: 1 200 000 + 450 000 = 1 650 000
        assertEquals(new BigDecimal("1650000"), reportData.totalEarned());
        assertEquals(new BigDecimal("1650000"), stats.totalIncome());

        // Verify active days matches: Math.max(3, 3) = 3
        assertEquals(3L, reportData.activeDays());
        assertEquals(3L, stats.activeDays());
    }
}
