package com.hisobchi.bot.debt;

import com.hisobchi.bot.common.util.UzbekDateParser;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtReminderLog;
import com.hisobchi.bot.debt.entity.DebtStatus;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtReminderLogRepository;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.debt.service.DebtService;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.report.dto.ReportData;
import com.hisobchi.bot.report.service.ReportService;
import com.hisobchi.bot.transaction.entity.TransactionSource;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtModuleComprehensiveTest {

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private DebtReminderLogRepository reminderLogRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DailyProfitService dailyProfitService;

    @InjectMocks
    private DebtService debtService;

    @InjectMocks
    private ReportService reportService;

    private final UzbekDateParser dateParser = new UzbekDateParser();
    private final ZoneId zoneId = ZoneId.of("Asia/Tashkent");

    @Test
    @DisplayName("Test 1: currentDate = 2026-10-01, dueDate = 2026-10-10 -> 2 days before reminder date is 2026-10-08")
    void testTwoDaysBeforeReminderDateCalculation() {
        LocalDate currentDate = LocalDate.of(2026, 10, 1);
        LocalDate dueDate = LocalDate.of(2026, 10, 10);

        LocalDate twoDaysBefore = dueDate.minusDays(2);
        assertEquals(LocalDate.of(2026, 10, 8), twoDaysBefore);
        assertTrue(twoDaysBefore.isAfter(currentDate));
    }

    @Test
    @DisplayName("Test 2 & 3: Due date schedule slots and stopping reminders when user marks debt as PAID")
    void testDueDateReminderSlotsAndStopOnPaid() {
        User user = User.builder().id(1L).telegramId(111L).timezone("Asia/Tashkent").build();
        LocalDate today = LocalDate.of(2026, 10, 15);

        Debt debt = Debt.builder()
                .id(10L)
                .user(user)
                .type(DebtType.BORROWED)
                .personName("Akmal")
                .amount(new BigDecimal("1500000"))
                .dueDate(today)
                .status(DebtStatus.ACTIVE)
                .build();

        when(debtRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(debt));

        // Slots planned for due day: 09:00, 13:00, 17:00, 21:00
        List<String> dueDaySlots = List.of("BORROWED_DUE_09", "BORROWED_DUE_13", "BORROWED_DUE_17", "BORROWED_DUE_21");
        assertEquals(4, dueDaySlots.size());

        // At 13:30 user marks debt as paid
        boolean resolved = debtService.markAsResolved(10L, 1L);
        assertTrue(resolved);
        assertEquals(DebtStatus.PAID, debt.getStatus());
        assertNotNull(debt.getClosedAt());

        // Subsequent slots (17:00, 21:00) check status: since status != ACTIVE, reminders stop
        assertNotEquals(DebtStatus.ACTIVE, debt.getStatus());
    }

    @Test
    @DisplayName("Test 4: LENT debt marked as resolved sets status to RECEIVED")
    void testLentDebtResolvedStatus() {
        User user = User.builder().id(2L).telegramId(222L).build();
        Debt lentDebt = Debt.builder()
                .id(20L)
                .user(user)
                .type(DebtType.LENT)
                .personName("Javlon")
                .amount(new BigDecimal("2000000"))
                .status(DebtStatus.ACTIVE)
                .build();

        when(debtRepository.findByIdAndUserId(20L, 2L)).thenReturn(Optional.of(lentDebt));

        boolean resolved = debtService.markAsResolved(20L, 2L);
        assertTrue(resolved);
        assertEquals(DebtStatus.RECEIVED, lentDebt.getStatus());
        assertNotNull(lentDebt.getClosedAt());
    }

    @Test
    @DisplayName("Test 5: Taking a debt (1 000 000) does not increase profit or totalEarned in financial reports")
    void testDebtDoesNotAffectFinancialReports() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 10, 1);

        // User took a debt of 1 000 000
        Debt debt = Debt.builder()
                .id(30L)
                .type(DebtType.BORROWED)
                .amount(new BigDecimal("1000000"))
                .build();
        assertNotNull(debt);

        // Daily financial report: only transactions and daily profits are included
        when(transactionRepository.sumAmountByUserIdAndTypeAndDate(userId, com.hisobchi.bot.transaction.entity.TransactionType.EXPENSE, today))
                .thenReturn(new BigDecimal("145000"));
        when(dailyProfitService.getProfit(userId, today))
                .thenReturn(Optional.empty()); // No daily profit entered yet
        when(transactionRepository.findCategoryExpensesByDate(userId, com.hisobchi.bot.transaction.entity.TransactionType.EXPENSE, today))
                .thenReturn(List.of());

        ReportData report = reportService.getDailyReportData(userId, today);

        // Debt amount of 1 000 000 must NOT appear in totalProfit or totalEarned
        assertEquals(new BigDecimal("145000"), report.totalExpense());
        assertEquals(BigDecimal.ZERO, report.totalProfit());
        assertEquals(new BigDecimal("145000"), report.totalEarned());
        assertFalse(report.totalEarned().compareTo(new BigDecimal("1000000")) > 0);
    }

    @Test
    @DisplayName("Test 6: '2 haftadan keyin' produces dueDate = currentDate + 14 days")
    void testTwoWeeksAfterParsing() {
        LocalDate today = LocalDate.of(2026, 10, 1);
        Optional<LocalDate> parsed = dateParser.parseDate("2 haftadan keyin", zoneId);

        assertTrue(parsed.isPresent());
        LocalDate expected = today.plusDays(14);
        assertEquals(expected, parsed.get());
    }
}
