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

    @Transactional
    public void resetAllUserData(Long userId) {
        log.info("Resetting all financial test data for user id: {}", userId);
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
}
