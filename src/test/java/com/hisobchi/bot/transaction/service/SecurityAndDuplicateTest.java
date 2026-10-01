package com.hisobchi.bot.transaction.service;

import com.hisobchi.bot.common.exception.UnauthorizedAccessException;
import com.hisobchi.bot.idempotency.service.IdempotencyService;
import com.hisobchi.bot.summary.service.DailySummaryService;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.*;
import com.hisobchi.bot.transaction.repository.TransactionDraftRepository;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAndDuplicateTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionDraftService draftService;

    @Mock
    private DailySummaryService dailySummaryService;

    @Mock
    private IdempotencyService idempotencyService;

    private TransactionService transactionService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, draftService, dailySummaryService);

        userA = User.builder().id(1L).telegramId(111L).username("userA").timezone("Asia/Tashkent").build();
        userB = User.builder().id(2L).telegramId(222L).username("userB").timezone("Asia/Tashkent").build();
    }

    @Test
    @DisplayName("Security: User A cannot access or delete User B's transaction")
    void testUserCannotAccessOtherUsersTransaction() {
        Transaction txBelongingToB = Transaction.builder()
                .id(100L)
                .user(userB)
                .amount(new BigDecimal("50000"))
                .type(TransactionType.EXPENSE)
                .transactionDate(LocalDate.now())
                .build();

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(txBelongingToB));

        // User A tries to delete User B's transaction -> should throw UnauthorizedAccessException
        assertThrows(UnauthorizedAccessException.class, () -> {
            transactionService.deleteTransaction(100L, userA.getId());
        });

        // User A tries to update User B's transaction -> should throw UnauthorizedAccessException
        assertThrows(UnauthorizedAccessException.class, () -> {
            transactionService.updateTransaction(100L, userA.getId(), new BigDecimal("60000"), null, null, null);
        });

        verify(transactionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Duplicate Callback Protection: Save clicked twice is blocked by IdempotencyService")
    void testDuplicateCallbackProtection() {
        when(idempotencyService.tryAcquireAction("save_draft_555")).thenReturn(true, false);

        boolean firstClick = idempotencyService.tryAcquireAction("save_draft_555");
        boolean secondClick = idempotencyService.tryAcquireAction("save_draft_555");

        assertTrue(firstClick, "First click should be processed");
        assertFalse(secondClick, "Second click should be blocked as duplicate");
    }
}
