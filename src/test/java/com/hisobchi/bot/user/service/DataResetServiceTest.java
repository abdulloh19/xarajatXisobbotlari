package com.hisobchi.bot.user.service;

import com.hisobchi.bot.debt.repository.DebtPaymentRepository;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.profit.repository.DailyProfitRepository;
import com.hisobchi.bot.summary.repository.DailySummaryRepository;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionDraftRepository;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserBalance;
import com.hisobchi.bot.user.repository.UserBalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataResetServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionDraftRepository draftRepository;
    @Mock
    private DailyProfitRepository dailyProfitRepository;
    @Mock
    private DailySummaryRepository dailySummaryRepository;
    @Mock
    private DebtPaymentRepository debtPaymentRepository;
    @Mock
    private DebtRepository debtRepository;
    @Mock
    private UserBalanceRepository userBalanceRepository;
    @Mock
    private com.hisobchi.bot.todo.repository.TodoReminderLogRepository todoReminderLogRepository;
    @Mock
    private com.hisobchi.bot.todo.repository.TodoSubtaskRepository todoSubtaskRepository;
    @Mock
    private com.hisobchi.bot.todo.repository.TodoTaskRepository todoTaskRepository;
    @Mock
    private com.hisobchi.bot.todo.repository.TodoProjectRepository todoProjectRepository;

    @InjectMocks
    private DataResetService dataResetService;

    private final Long userId = 100L;

    @Test
    @DisplayName("deleteAllProfitsAndIncomes should delete profits, income txs, drafts, and reset balance")
    void testDeleteAllProfitsAndIncomes() {
        User user = User.builder().id(userId).build();
        UserBalance balance = UserBalance.builder()
                .user(user)
                .cashBalance(new BigDecimal("500000"))
                .cardBalance(new BigDecimal("300000"))
                .build();
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));

        dataResetService.deleteAllProfitsAndIncomes(userId);

        verify(dailyProfitRepository).deleteByUserId(userId);
        verify(transactionRepository).deleteByUserIdAndType(userId, TransactionType.INCOME);
        verify(draftRepository).deleteByUserIdAndType(userId, TransactionType.INCOME);
        verify(dailySummaryRepository).deleteByUserId(userId);
        verify(userBalanceRepository).save(balance);

        assertEquals(BigDecimal.ZERO, balance.getCashBalance());
        assertEquals(BigDecimal.ZERO, balance.getCardBalance());
    }

    @Test
    @DisplayName("deleteAllExpenses should delete expense txs and drafts")
    void testDeleteAllExpenses() {
        dataResetService.deleteAllExpenses(userId);

        verify(transactionRepository).deleteByUserIdAndType(userId, TransactionType.EXPENSE);
        verify(draftRepository).deleteByUserIdAndType(userId, TransactionType.EXPENSE);
        verify(dailySummaryRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("resetAllUserData should wipe everything and reset balances")
    void testResetAllUserData() {
        User user = User.builder().id(userId).build();
        UserBalance balance = UserBalance.builder()
                .user(user)
                .cashBalance(new BigDecimal("200000"))
                .cardBalance(new BigDecimal("100000"))
                .build();
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));

        dataResetService.resetAllUserData(userId);

        verify(todoReminderLogRepository).deleteByUserId(userId);
        verify(todoSubtaskRepository).deleteByUserId(userId);
        verify(todoTaskRepository).deleteByUserId(userId);
        verify(todoProjectRepository).deleteByUserId(userId);
        verify(draftRepository).deleteByUserId(userId);
        verify(debtPaymentRepository).deleteByUserId(userId);
        verify(debtRepository).deleteByUserId(userId);
        verify(transactionRepository).deleteByUserId(userId);
        verify(dailyProfitRepository).deleteByUserId(userId);
        verify(dailySummaryRepository).deleteByUserId(userId);
        verify(userBalanceRepository).save(balance);

        assertEquals(BigDecimal.ZERO, balance.getCashBalance());
        assertEquals(BigDecimal.ZERO, balance.getCardBalance());
    }
}
