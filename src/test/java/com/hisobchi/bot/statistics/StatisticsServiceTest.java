package com.hisobchi.bot.statistics;

import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.dto.MonthlyStatisticsDto;
import com.hisobchi.bot.statistics.dto.WeeklyStatisticsDto;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DailySummaryService dailySummaryService;

    @Mock
    private DailyProfitService dailyProfitService;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private com.hisobchi.bot.debt.repository.DebtPaymentRepository debtPaymentRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    @DisplayName("Misol 1: Foyda = 700 000, Xarajat = 125 000 -> Umumiy ishlab topilgan = 825 000, Foyda = 700 000")
    void testDailyStatisticsExample1() {
        User user = User.builder().id(1L).build();
        LocalDate today = LocalDate.of(2026, 10, 2);

        when(transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.EXPENSE, today))
                .thenReturn(new BigDecimal("125000"));

        DailyProfit profit = DailyProfit.builder()
                .profitDate(today)
                .totalProfit(new BigDecimal("700000"))
                .cashAmount(new BigDecimal("500000"))
                .cardAmount(new BigDecimal("200000"))
                .build();
        when(dailyProfitService.getProfit(user.getId(), today)).thenReturn(Optional.of(profit));
        when(transactionRepository.countByUserIdAndDate(user.getId(), today)).thenReturn(4L);
        when(transactionRepository.findCategoryExpensesByDate(user.getId(), TransactionType.EXPENSE, today)).thenReturn(List.of());
        when(dailySummaryService.isDayClosed(user.getId(), today)).thenReturn(false);

        DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

        assertEquals(new BigDecimal("825000"), stats.totalIncome(), "Umumiy ishlab topilgan (totalEarned) 825 000 bo'lishi kerak");
        assertEquals(new BigDecimal("125000"), stats.totalExpense(), "Xarajatlar 125 000 bo'lishi kerak");
        assertEquals(new BigDecimal("700000"), stats.netProfit(), "Bugungi foydangiz (netProfit) 700 000 bo'lishi kerak (hech qachon 575 000 emas)");
        assertEquals(4L, stats.transactionCount());
    }

    @Test
    @DisplayName("Misol 2: Foyda = 500 000, Xarajat = 200 000 -> Umumiy ishlab topilgan = 700 000, Foyda = 500 000")
    void testDailyStatisticsExample2() {
        User user = User.builder().id(1L).build();
        LocalDate today = LocalDate.of(2026, 10, 2);

        when(transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.EXPENSE, today))
                .thenReturn(new BigDecimal("200000"));

        DailyProfit profit = DailyProfit.builder()
                .profitDate(today)
                .totalProfit(new BigDecimal("500000"))
                .build();
        when(dailyProfitService.getProfit(user.getId(), today)).thenReturn(Optional.of(profit));
        when(transactionRepository.countByUserIdAndDate(user.getId(), today)).thenReturn(2L);
        when(transactionRepository.findCategoryExpensesByDate(user.getId(), TransactionType.EXPENSE, today)).thenReturn(List.of());
        when(dailySummaryService.isDayClosed(user.getId(), today)).thenReturn(false);

        DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

        assertEquals(new BigDecimal("700000"), stats.totalIncome());
        assertEquals(new BigDecimal("200000"), stats.totalExpense());
        assertEquals(new BigDecimal("500000"), stats.netProfit());
    }

    @Test
    @DisplayName("Weekly & Monthly Statistics use totalEarned = profit + expense, netProfit = totalProfit")
    void testWeeklyStatisticsFormula() {
        User user = User.builder().id(1L).build();
        LocalDate refDate = LocalDate.of(2026, 10, 2);
        LocalDate start = LocalDate.of(2026, 9, 28);
        LocalDate end = LocalDate.of(2026, 10, 4);

        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("300000"));

        List<DailyProfit> profits = List.of(
                DailyProfit.builder().profitDate(start).totalProfit(new BigDecimal("700000")).build(),
                DailyProfit.builder().profitDate(end).totalProfit(new BigDecimal("500000")).build()
        );
        when(dailyProfitService.getProfitsBetween(user.getId(), start, end)).thenReturn(profits);
        when(transactionRepository.countByUserIdAndDateBetween(user.getId(), start, end)).thenReturn(5L);
        when(transactionRepository.findCategoryExpensesBetween(user.getId(), TransactionType.EXPENSE, start, end)).thenReturn(List.of());

        WeeklyStatisticsDto stats = statisticsService.getWeeklyStatistics(user, refDate);

        // totalProfit = 700 000 + 500 000 = 1 200 000
        // totalEarned = 1 200 000 + 300 000 = 1 500 000
        assertEquals(new BigDecimal("1500000"), stats.totalIncome(), "Weekly totalEarned");
        assertEquals(new BigDecimal("300000"), stats.totalExpense(), "Weekly totalExpense");
        assertEquals(new BigDecimal("1200000"), stats.netProfit(), "Weekly netProfit");
    }

    @Test
    @DisplayName("Lent debt is included in totalExpense and categories in daily statistics")
    void testDailyStatisticsWithLentDebt() {
        User user = User.builder().id(1L).build();
        LocalDate today = LocalDate.of(2026, 10, 2);

        when(transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.EXPENSE, today))
                .thenReturn(new BigDecimal("50000"));
        when(debtRepository.sumCreatedAmountByUserIdAndTypeAndDate(user.getId(), DebtType.LENT, today))
                .thenReturn(new BigDecimal("200000"));
        when(debtRepository.countCreatedByUserIdAndTypeAndDate(user.getId(), DebtType.LENT, today))
                .thenReturn(1L);

        DailyProfit profit = DailyProfit.builder()
                .profitDate(today)
                .totalProfit(new BigDecimal("300000"))
                .build();
        when(dailyProfitService.getProfit(user.getId(), today)).thenReturn(Optional.of(profit));
        when(transactionRepository.countByUserIdAndDate(user.getId(), today)).thenReturn(2L);
        when(transactionRepository.findCategoryExpensesByDate(user.getId(), TransactionType.EXPENSE, today)).thenReturn(List.of());
        when(dailySummaryService.isDayClosed(user.getId(), today)).thenReturn(false);

        DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

        // totalExpense = 50 000 + 200 000 (lent debt) = 250 000
        assertEquals(new BigDecimal("250000"), stats.totalExpense());
        // totalEarned = 300 000 (profit) + 250 000 (expenses) = 550 000
        assertEquals(new BigDecimal("550000"), stats.totalIncome());
        assertEquals(new BigDecimal("300000"), stats.netProfit());
        assertEquals(3L, stats.transactionCount()); // 2 tx + 1 lent debt

        boolean hasLentCategory = stats.expenseCategories().stream()
                .anyMatch(c -> "Berilgan qarz".equals(c.categoryName()) && c.totalAmount().compareTo(new BigDecimal("200000")) == 0);
        assertTrue(hasLentCategory, "Berilgan qarz xarajatlar ro'yxatida bo'lishi kerak");
    }

    @Test
    @DisplayName("Paid debt (repayment) is included in totalExpense and categories in daily statistics")
    void testDailyStatisticsWithPaidDebt() {
        User user = User.builder().id(1L).build();
        LocalDate today = LocalDate.of(2026, 10, 2);

        when(transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.EXPENSE, today))
                .thenReturn(new BigDecimal("70000"));
        when(debtPaymentRepository.sumAmountByUserIdAndPaymentTypeAndPaymentDate(user.getId(), com.hisobchi.bot.debt.entity.DebtPaymentType.DEBT_PAYMENT, today))
                .thenReturn(new BigDecimal("150000"));
        when(debtPaymentRepository.countByUserIdAndPaymentTypeAndPaymentDate(user.getId(), com.hisobchi.bot.debt.entity.DebtPaymentType.DEBT_PAYMENT, today))
                .thenReturn(1L);

        DailyProfit profit = DailyProfit.builder()
                .profitDate(today)
                .totalProfit(new BigDecimal("400000"))
                .build();
        when(dailyProfitService.getProfit(user.getId(), today)).thenReturn(Optional.of(profit));
        when(transactionRepository.countByUserIdAndDate(user.getId(), today)).thenReturn(3L);
        when(transactionRepository.findCategoryExpensesByDate(user.getId(), TransactionType.EXPENSE, today)).thenReturn(List.of());
        when(dailySummaryService.isDayClosed(user.getId(), today)).thenReturn(false);

        DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

        // totalExpense = 70 000 + 150 000 (paid debt) = 220 000
        assertEquals(new BigDecimal("220000"), stats.totalExpense());
        // totalEarned = 400 000 (profit) + 220 000 (expenses) = 620 000
        assertEquals(new BigDecimal("620000"), stats.totalIncome());
        assertEquals(new BigDecimal("400000"), stats.netProfit());
        assertEquals(4L, stats.transactionCount()); // 3 tx + 1 paid debt

        boolean hasPaidDebtCategory = stats.expenseCategories().stream()
                .anyMatch(c -> "To‘langan qarz".equals(c.categoryName()) && c.totalAmount().compareTo(new BigDecimal("150000")) == 0);
        assertTrue(hasPaidDebtCategory, "To‘langan qarz xarajatlar ro'yxatida bo'lishi kerak");
    }
}
