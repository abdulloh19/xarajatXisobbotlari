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
}
