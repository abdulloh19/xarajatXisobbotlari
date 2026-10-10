package com.hisobchi.bot.report;

import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.report.dto.ReportData;
import com.hisobchi.bot.report.service.ReportService;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DailyProfitService dailyProfitService;

    @Mock
    private com.hisobchi.bot.debt.repository.DebtRepository debtRepository;

    @Mock
    private com.hisobchi.bot.debt.repository.DebtPaymentRepository debtPaymentRepository;

    @Mock
    private com.hisobchi.bot.user.service.BalanceService balanceService;

    @InjectMocks
    private ReportService reportService;

    private final Long userId = 1L;
    private final LocalDate today = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("Test 1: expense = 145000, profit = 200000 -> dailyEarned = 345000")
    void testDailyEarnedCalculation() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDate(userId, TransactionType.EXPENSE, today))
                .thenReturn(new BigDecimal("145000"));

        DailyProfit profit = DailyProfit.builder()
                .profitDate(today)
                .cashAmount(new BigDecimal("120000"))
                .cardAmount(new BigDecimal("80000"))
                .totalProfit(new BigDecimal("200000"))
                .build();

        when(dailyProfitService.getProfit(userId, today))
                .thenReturn(Optional.of(profit));
        when(transactionRepository.findCategoryExpensesByDate(userId, TransactionType.EXPENSE, today))
                .thenReturn(List.of());

        ReportData report = reportService.getDailyReportData(userId, today);

        assertEquals(new BigDecimal("145000"), report.totalExpense());
        assertEquals(new BigDecimal("200000"), report.totalProfit());
        assertEquals(new BigDecimal("345000"), report.totalEarned());
    }

    @Test
    @DisplayName("Test 2 & 3: 7-day period with incomplete day tracking")
    void testWeeklyTotalsAndIncompleteDays() {
        LocalDate start = LocalDate.of(2026, 9, 28);
        LocalDate end = LocalDate.of(2026, 10, 4);

        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(userId, TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("950000"));

        // 6 days with profit, 1 day without profit
        List<DailyProfit> profits = List.of(
                DailyProfit.builder().profitDate(start).totalProfit(new BigDecimal("200000")).cashAmount(BigDecimal.ZERO).cardAmount(BigDecimal.ZERO).build(),
                DailyProfit.builder().profitDate(start.plusDays(1)).totalProfit(new BigDecimal("250000")).cashAmount(BigDecimal.ZERO).cardAmount(BigDecimal.ZERO).build(),
                DailyProfit.builder().profitDate(start.plusDays(2)).totalProfit(new BigDecimal("300000")).cashAmount(BigDecimal.ZERO).cardAmount(BigDecimal.ZERO).build(),
                DailyProfit.builder().profitDate(start.plusDays(3)).totalProfit(new BigDecimal("220000")).cashAmount(BigDecimal.ZERO).cardAmount(BigDecimal.ZERO).build(),
                DailyProfit.builder().profitDate(start.plusDays(4)).totalProfit(new BigDecimal("180000")).cashAmount(BigDecimal.ZERO).cardAmount(BigDecimal.ZERO).build(),
                DailyProfit.builder().profitDate(start.plusDays(5)).totalProfit(new BigDecimal("350000")).cashAmount(BigDecimal.ZERO).cardAmount(BigDecimal.ZERO).build()
                // Day 6 (end) has no profit recorded!
        );

        when(dailyProfitService.getProfitsBetween(userId, start, end)).thenReturn(profits);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDate(eq(userId), eq(TransactionType.EXPENSE), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate date = inv.getArgument(2);
                    return date.equals(end) ? new BigDecimal("50000") : BigDecimal.ZERO;
                });
        when(transactionRepository.countActiveDaysBetween(userId, start, end)).thenReturn(7L);
        when(transactionRepository.findCategoryExpensesBetween(userId, TransactionType.EXPENSE, start, end))
                .thenReturn(List.of());

        ReportData report = reportService.getPeriodReportData(userId, start, end, "HAFTALIK HISOBOT");

        // Sum of profits = 200000+250000+300000+220000+180000+350000 = 1500000
        // totalEarned = 950000 + 1500000 = 2450000
        assertEquals(new BigDecimal("1500000"), report.totalProfit());
        assertEquals(new BigDecimal("2450000"), report.totalEarned());
        assertEquals(6, report.completedDays());
        assertEquals(1, report.incompleteDays());
        assertEquals(List.of(end), report.incompleteDates());
    }
}
