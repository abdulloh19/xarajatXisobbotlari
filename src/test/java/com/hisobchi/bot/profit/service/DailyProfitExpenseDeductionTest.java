package com.hisobchi.bot.profit.service;

import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.repository.DailyProfitRepository;
import com.hisobchi.bot.summary.service.DailySummaryService;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardMarkup;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.*;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.transaction.service.TransactionDraftService;
import com.hisobchi.bot.transaction.service.TransactionService;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.service.BalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyProfitExpenseDeductionTest {

    @Mock
    private DailyProfitRepository dailyProfitRepository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionDraftService draftService;

    @Mock
    private DailySummaryService dailySummaryService;

    private DailyProfitService dailyProfitService;
    private TransactionService transactionService;
    private InlineKeyboardFactory inlineKeyboardFactory;
    private User testUser;

    @BeforeEach
    void setUp() {
        dailyProfitService = new DailyProfitService(dailyProfitRepository, balanceService);
        transactionService = new TransactionService(transactionRepository, draftService, dailySummaryService);
        inlineKeyboardFactory = new InlineKeyboardFactory();

        testUser = User.builder()
                .id(1L)
                .telegramId(12345L)
                .timezone("Asia/Tashkent")
                .build();
    }

    @Test
    @DisplayName("Deduct expense from daily profit when cash is sufficient")
    void testDeductFromProfitSufficientCash() {
        LocalDate date = LocalDate.now();
        DailyProfit existingProfit = DailyProfit.builder()
                .id(10L)
                .user(testUser)
                .profitDate(date)
                .cashAmount(new BigDecimal("100000"))
                .cardAmount(new BigDecimal("50000"))
                .totalProfit(new BigDecimal("150000"))
                .build();

        when(dailyProfitRepository.findByUserIdAndProfitDate(testUser.getId(), date))
                .thenReturn(Optional.of(existingProfit));
        when(dailyProfitRepository.save(any(DailyProfit.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        dailyProfitService.deductFromProfit(testUser, date, new BigDecimal("30000"));

        ArgumentCaptor<DailyProfit> captor = ArgumentCaptor.forClass(DailyProfit.class);
        verify(dailyProfitRepository, atLeastOnce()).save(captor.capture());

        DailyProfit saved = captor.getValue();
        assertEquals(new BigDecimal("70000"), saved.getCashAmount());
        assertEquals(new BigDecimal("50000"), saved.getCardAmount());
        assertEquals(new BigDecimal("120000"), saved.getTotalProfit());
    }

    @Test
    @DisplayName("Deduct expense from daily profit when cash is insufficient covers from card")
    void testDeductFromProfitDeficitCoversFromCard() {
        LocalDate date = LocalDate.now();
        DailyProfit existingProfit = DailyProfit.builder()
                .id(10L)
                .user(testUser)
                .profitDate(date)
                .cashAmount(new BigDecimal("20000"))
                .cardAmount(new BigDecimal("50000"))
                .totalProfit(new BigDecimal("70000"))
                .build();

        when(dailyProfitRepository.findByUserIdAndProfitDate(testUser.getId(), date))
                .thenReturn(Optional.of(existingProfit));
        when(dailyProfitRepository.save(any(DailyProfit.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        dailyProfitService.deductFromProfit(testUser, date, new BigDecimal("30000"));

        ArgumentCaptor<DailyProfit> captor = ArgumentCaptor.forClass(DailyProfit.class);
        verify(dailyProfitRepository, atLeastOnce()).save(captor.capture());

        DailyProfit saved = captor.getValue();
        assertEquals(BigDecimal.ZERO, saved.getCashAmount());
        assertEquals(new BigDecimal("40000"), saved.getCardAmount());
        assertEquals(new BigDecimal("40000"), saved.getTotalProfit());
    }

    @Test
    @DisplayName("confirmAndSaveWithDate saves transaction with specified target date (yesterday)")
    void testConfirmAndSaveWithDateYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        TransactionDraft draft = TransactionDraft.builder()
                .id(99L)
                .user(testUser)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("45000"))
                .currency("UZS")
                .status(DraftStatus.PENDING)
                .source(TransactionSource.TEXT)
                .build();

        when(draftService.getDraft(99L, testUser.getId())).thenReturn(draft);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(555L);
            return t;
        });

        TransactionDto dto = transactionService.confirmAndSaveWithDate(99L, testUser.getId(), yesterday);

        assertNotNull(dto);
        assertEquals(yesterday, dto.transactionDate());
        assertEquals(new BigDecimal("45000"), dto.amount());
        verify(draftService, times(1)).markConfirmed(draft);
    }

    @Test
    @DisplayName("Expense confirmation keyboard presents yesterday and today buttons")
    void testExpenseConfirmationKeyboard() {
        InlineKeyboardMarkup keyboard = inlineKeyboardFactory.getDraftConfirmationKeyboard(101L, TransactionType.EXPENSE);

        assertNotNull(keyboard);
        assertNotNull(keyboard.getInlineKeyboard());
        assertEquals(2, keyboard.getInlineKeyboard().size());

        // First row: yesterday and today buttons
        var firstRow = keyboard.getInlineKeyboard().get(0);
        assertEquals(2, firstRow.size());
        assertEquals("draft:save_yesterday:101", firstRow.get(0).getCallbackData());
        assertEquals("draft:save_today:101", firstRow.get(1).getCallbackData());
    }

    @Test
    @DisplayName("Expense confirmation keyboard on off-day only offers previous worked day option")
    void testOffDayExpenseConfirmationKeyboard() {
        InlineKeyboardMarkup keyboard = inlineKeyboardFactory.getDraftConfirmationKeyboard(101L, TransactionType.EXPENSE, true, "03.10");

        assertNotNull(keyboard);
        var rows = keyboard.getInlineKeyboard();
        assertEquals(2, rows.size());

        // First row: only previous worked day button
        var firstRow = rows.get(0);
        assertEquals(1, firstRow.size());
        assertEquals("draft:save_yesterday:101", firstRow.get(0).getCallbackData());
        assertTrue(firstRow.get(0).getText().contains("Oldingi ishlagan kundan"));
        assertTrue(firstRow.get(0).getText().contains("03.10"));
    }

    @Test
    @DisplayName("Work day confirmation keyboard has yes and no buttons")
    void testWorkDayConfirmationKeyboard() {
        InlineKeyboardMarkup keyboard = inlineKeyboardFactory.getWorkDayConfirmationKeyboard();

        assertNotNull(keyboard);
        var rows = keyboard.getInlineKeyboard();
        assertEquals(2, rows.size());

        var firstRow = rows.get(0);
        assertEquals(2, firstRow.size());
        assertEquals("profit:work:yes", firstRow.get(0).getCallbackData());
        assertEquals("profit:work:no", firstRow.get(1).getCallbackData());
    }

    @Test
    @DisplayName("markOffDay sets isWorkDay=false and zeroes profit")
    void testMarkOffDay() {
        LocalDate today = LocalDate.now();
        DailyProfit existing = DailyProfit.builder()
                .id(1L)
                .user(testUser)
                .profitDate(today)
                .cashAmount(new BigDecimal("50000"))
                .cardAmount(new BigDecimal("30000"))
                .totalProfit(new BigDecimal("80000"))
                .isWorkDay(true)
                .build();

        when(dailyProfitRepository.findByUserIdAndProfitDate(testUser.getId(), today))
                .thenReturn(Optional.of(existing));
        when(dailyProfitRepository.save(any(DailyProfit.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DailyProfit saved = dailyProfitService.markOffDay(testUser, today);

        assertNotNull(saved);
        assertFalse(saved.isWorkDay());
        assertEquals(BigDecimal.ZERO, saved.getCashAmount());
        assertEquals(BigDecimal.ZERO, saved.getCardAmount());
        assertEquals(BigDecimal.ZERO, saved.getTotalProfit());
        verify(balanceService, times(1)).updateDailyProfitDelta(testUser, new BigDecimal("-50000"), new BigDecimal("-30000"));
    }

    @Test
    @DisplayName("getLastWorkedDayProfit retrieves the most recent working day with profit")
    void testGetLastWorkedDayProfit() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        LocalDate offDayYesterday = LocalDate.of(2026, 10, 3);
        LocalDate workedDayBefore = LocalDate.of(2026, 10, 2);

        DailyProfit pOff = DailyProfit.builder()
                .user(testUser)
                .profitDate(offDayYesterday)
                .totalProfit(BigDecimal.ZERO)
                .isWorkDay(false)
                .build();

        DailyProfit pWorked = DailyProfit.builder()
                .user(testUser)
                .profitDate(workedDayBefore)
                .cashAmount(new BigDecimal("200000"))
                .totalProfit(new BigDecimal("200000"))
                .isWorkDay(true)
                .build();

        when(dailyProfitRepository.findAllByUserIdAndProfitDateLessThanOrderByProfitDateDesc(testUser.getId(), today))
                .thenReturn(java.util.List.of(pOff, pWorked));

        Optional<DailyProfit> result = dailyProfitService.getLastWorkedDayProfit(testUser.getId(), today);

        assertTrue(result.isPresent());
        assertEquals(workedDayBefore, result.get().getProfitDate());
        assertEquals(new BigDecimal("200000"), result.get().getTotalProfit());
    }

    @Test
    @DisplayName("subtractFromProfit: 500 000 minus 100 000 results in 400 000 profit")
    void testSubtractFromProfit() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        DailyProfit current = DailyProfit.builder()
                .id(10L)
                .user(testUser)
                .profitDate(today)
                .cashAmount(new BigDecimal("500000"))
                .cardAmount(BigDecimal.ZERO)
                .totalProfit(new BigDecimal("500000"))
                .isWorkDay(true)
                .build();

        when(dailyProfitRepository.findByUserIdAndProfitDate(testUser.getId(), today))
                .thenReturn(Optional.of(current));
        when(dailyProfitRepository.save(any(DailyProfit.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DailyProfit updated = dailyProfitService.subtractFromProfit(testUser, today, new BigDecimal("100000"));

        assertNotNull(updated);
        assertEquals(new BigDecimal("400000"), updated.getCashAmount());
        assertEquals(new BigDecimal("400000"), updated.getTotalProfit());
        verify(balanceService).updateDailyProfitDelta(eq(testUser), eq(new BigDecimal("-100000")), eq(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("addToProfit: 500 000 plus 100 000 results in 600 000 profit")
    void testAddToProfit() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        DailyProfit current = DailyProfit.builder()
                .id(10L)
                .user(testUser)
                .profitDate(today)
                .cashAmount(new BigDecimal("500000"))
                .cardAmount(BigDecimal.ZERO)
                .totalProfit(new BigDecimal("500000"))
                .isWorkDay(true)
                .build();

        when(dailyProfitRepository.findByUserIdAndProfitDate(testUser.getId(), today))
                .thenReturn(Optional.of(current));
        when(dailyProfitRepository.save(any(DailyProfit.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DailyProfit updated = dailyProfitService.addToProfit(testUser, today, new BigDecimal("100000"));

        assertNotNull(updated);
        assertEquals(new BigDecimal("600000"), updated.getCashAmount());
        assertEquals(new BigDecimal("600000"), updated.getTotalProfit());
        verify(balanceService).updateDailyProfitDelta(eq(testUser), eq(new BigDecimal("100000")), eq(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Expense confirmation keyboard when profit is entered offers deduct and separate buttons")
    void testExpenseConfirmationKeyboardWhenProfitEnteredToday() {
        InlineKeyboardMarkup keyboard = inlineKeyboardFactory.getDraftConfirmationKeyboard(101L, TransactionType.EXPENSE, false, null, true);

        assertNotNull(keyboard);
        assertNotNull(keyboard.getInlineKeyboard());
        assertEquals(3, keyboard.getInlineKeyboard().size());

        // First row: deduct from today's profit OR separate expense
        var firstRow = keyboard.getInlineKeyboard().get(0);
        assertEquals(2, firstRow.size());
        assertEquals("draft:save_today_deduct:101", firstRow.get(0).getCallbackData());
        assertTrue(firstRow.get(0).getText().contains("Kunlik foydadan"));
        assertEquals("draft:save_today_separate:101", firstRow.get(1).getCallbackData());
        assertTrue(firstRow.get(1).getText().contains("Alohida"));

        // Second row: oldingi kundan
        var secondRow = keyboard.getInlineKeyboard().get(1);
        assertEquals(1, secondRow.size());
        assertEquals("draft:save_yesterday:101", secondRow.get(0).getCallbackData());
    }

    @Test
    @DisplayName("Main menu return keyboard contains menu:main callback")
    void testMainMenuReturnKeyboard() {
        InlineKeyboardMarkup keyboard = inlineKeyboardFactory.getMainMenuReturnKeyboard();
        assertNotNull(keyboard);
        assertEquals(1, keyboard.getInlineKeyboard().size());
        assertEquals("menu:main", keyboard.getInlineKeyboard().get(0).get(0).getCallbackData());
        assertTrue(keyboard.getInlineKeyboard().get(0).get(0).getText().contains("Asosiy menyuga qaytish"));
    }
}
