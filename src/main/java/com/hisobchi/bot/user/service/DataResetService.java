package com.hisobchi.bot.user.service;

import com.hisobchi.bot.debt.repository.DebtPaymentRepository;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.profit.repository.DailyProfitRepository;
import com.hisobchi.bot.summary.repository.DailySummaryRepository;
import com.hisobchi.bot.transaction.repository.TransactionDraftRepository;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.repository.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataResetService {

    private final TransactionRepository transactionRepository;
    private final TransactionDraftRepository draftRepository;
    private final DailyProfitRepository dailyProfitRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final DebtPaymentRepository debtPaymentRepository;
    private final DebtRepository debtRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final com.hisobchi.bot.todo.repository.TodoReminderLogRepository todoReminderLogRepository;
    private final com.hisobchi.bot.todo.repository.TodoSubtaskRepository todoSubtaskRepository;
    private final com.hisobchi.bot.todo.repository.TodoTaskRepository todoTaskRepository;
    private final com.hisobchi.bot.todo.repository.TodoProjectRepository todoProjectRepository;

    @Transactional
    public void resetAllUserData(Long userId) {
        log.info("Resetting all financial test data for user id: {}", userId);
        todoReminderLogRepository.deleteByUserId(userId);
        todoSubtaskRepository.deleteByUserId(userId);
        todoTaskRepository.deleteByUserId(userId);
        todoProjectRepository.deleteByUserId(userId);
        draftRepository.deleteByUserId(userId);
        debtPaymentRepository.deleteByUserId(userId);
        debtRepository.deleteByUserId(userId);
        transactionRepository.deleteByUserId(userId);
        dailyProfitRepository.deleteByUserId(userId);
        dailySummaryRepository.deleteByUserId(userId);

        userBalanceRepository.findByUserId(userId).ifPresent(ub -> {
            ub.setCashBalance(BigDecimal.ZERO);
            ub.setCardBalance(BigDecimal.ZERO);
            userBalanceRepository.save(ub);
        });
        log.info("User id {} data reset successfully.", userId);
    }

    @Transactional
    public void deleteAllProfitsAndIncomes(Long userId) {
        log.info("Deleting all daily profits and incomes for user id: {}", userId);
        dailyProfitRepository.deleteByUserId(userId);
        transactionRepository.deleteByUserIdAndType(userId, com.hisobchi.bot.transaction.entity.TransactionType.INCOME);
        draftRepository.deleteByUserIdAndType(userId, com.hisobchi.bot.transaction.entity.TransactionType.INCOME);
        dailySummaryRepository.deleteByUserId(userId);

        userBalanceRepository.findByUserId(userId).ifPresent(ub -> {
            ub.setCashBalance(BigDecimal.ZERO);
            ub.setCardBalance(BigDecimal.ZERO);
            userBalanceRepository.save(ub);
        });
        log.info("All profits and incomes deleted for user id: {}", userId);
    }

    @Transactional
    public void deleteAllExpenses(Long userId) {
        log.info("Deleting all expenses for user id: {}", userId);
        transactionRepository.deleteByUserIdAndType(userId, com.hisobchi.bot.transaction.entity.TransactionType.EXPENSE);
        draftRepository.deleteByUserIdAndType(userId, com.hisobchi.bot.transaction.entity.TransactionType.EXPENSE);
        dailySummaryRepository.deleteByUserId(userId);
        log.info("All expenses deleted for user id: {}", userId);
    }
}
