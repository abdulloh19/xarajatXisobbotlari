package com.hisobchi.bot.user.service;

import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserBalance;
import com.hisobchi.bot.user.repository.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserBalanceRepository balanceRepository;

    @Transactional
    public UserBalance getOrCreateBalance(User user) {
        return balanceRepository.findByUserId(user.getId())
                .orElseGet(() -> balanceRepository.save(
                        UserBalance.builder()
                                .user(user)
                                .cashBalance(BigDecimal.ZERO)
                                .cardBalance(BigDecimal.ZERO)
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public Optional<UserBalance> getBalance(Long userId) {
        return balanceRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(Long userId) {
        return getBalance(userId)
                .map(UserBalance::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public UserBalance updateDailyProfitDelta(User user, BigDecimal deltaCash, BigDecimal deltaCard) {
        UserBalance balance = getOrCreateBalance(user);
        BigDecimal safeDeltaCash = deltaCash != null ? deltaCash : BigDecimal.ZERO;
        BigDecimal safeDeltaCard = deltaCard != null ? deltaCard : BigDecimal.ZERO;

        balance.setCashBalance(balance.getCashBalance().add(safeDeltaCash));
        balance.setCardBalance(balance.getCardBalance().add(safeDeltaCard));
        return balanceRepository.save(balance);
    }

    @Transactional
    public UserBalance applyLentDebt(User user, BigDecimal amount, String paymentMethod) {
        UserBalance balance = getOrCreateBalance(user);
        BigDecimal safeAmt = amount != null ? amount : BigDecimal.ZERO;
        if (isCard(paymentMethod)) {
            balance.setCardBalance(balance.getCardBalance().subtract(safeAmt));
        } else {
            balance.setCashBalance(balance.getCashBalance().subtract(safeAmt));
        }
        log.info("Applied LENT debt: user={}, amount={}, method={}, newCash={}, newCard={}, totalAvailable={}",
                user.getId(), safeAmt, paymentMethod, balance.getCashBalance(), balance.getCardBalance(), balance.getAvailableBalance());
        return balanceRepository.save(balance);
    }

    @Transactional
    public UserBalance applyBorrowedDebt(User user, BigDecimal amount, String paymentMethod) {
        UserBalance balance = getOrCreateBalance(user);
        BigDecimal safeAmt = amount != null ? amount : BigDecimal.ZERO;
        if (isCard(paymentMethod)) {
            balance.setCardBalance(balance.getCardBalance().add(safeAmt));
        } else {
            balance.setCashBalance(balance.getCashBalance().add(safeAmt));
        }
        log.info("Applied BORROWED debt: user={}, amount={}, method={}, newCash={}, newCard={}, totalAvailable={}",
                user.getId(), safeAmt, paymentMethod, balance.getCashBalance(), balance.getCardBalance(), balance.getAvailableBalance());
        return balanceRepository.save(balance);
    }

    @Transactional
    public UserBalance applyDebtPayment(User user, BigDecimal paymentAmount, String paymentMethod) {
        UserBalance balance = getOrCreateBalance(user);
        BigDecimal safeAmt = paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
        if (isCard(paymentMethod)) {
            balance.setCardBalance(balance.getCardBalance().subtract(safeAmt));
        } else {
            balance.setCashBalance(balance.getCashBalance().subtract(safeAmt));
        }
        log.info("Applied DEBT_PAYMENT: user={}, amount={}, method={}, newCash={}, newCard={}, totalAvailable={}",
                user.getId(), safeAmt, paymentMethod, balance.getCashBalance(), balance.getCardBalance(), balance.getAvailableBalance());
        return balanceRepository.save(balance);
    }

    @Transactional
    public UserBalance applyDebtReturn(User user, BigDecimal returnedAmount, String paymentMethod) {
        UserBalance balance = getOrCreateBalance(user);
        BigDecimal safeAmt = returnedAmount != null ? returnedAmount : BigDecimal.ZERO;
        if (isCard(paymentMethod)) {
            balance.setCardBalance(balance.getCardBalance().add(safeAmt));
        } else {
            balance.setCashBalance(balance.getCashBalance().add(safeAmt));
        }
        log.info("Applied DEBT_RETURN: user={}, amount={}, method={}, newCash={}, newCard={}, totalAvailable={}",
                user.getId(), safeAmt, paymentMethod, balance.getCashBalance(), balance.getCardBalance(), balance.getAvailableBalance());
        return balanceRepository.save(balance);
    }

    @Transactional
    public UserBalance deductExpenseFromBalance(User user, BigDecimal amount) {
        UserBalance balance = getOrCreateBalance(user);
        BigDecimal safeAmt = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal currentCash = balance.getCashBalance() != null ? balance.getCashBalance() : BigDecimal.ZERO;
        BigDecimal currentCard = balance.getCardBalance() != null ? balance.getCardBalance() : BigDecimal.ZERO;

        BigDecimal newCash = currentCash.subtract(safeAmt);
        BigDecimal newCard = currentCard;
        if (newCash.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal deficit = newCash.abs();
            newCash = BigDecimal.ZERO;
            newCard = currentCard.subtract(deficit);
            if (newCard.compareTo(BigDecimal.ZERO) < 0) {
                newCard = BigDecimal.ZERO;
            }
        }
        balance.setCashBalance(newCash);
        balance.setCardBalance(newCard);
        UserBalance saved = balanceRepository.save(balance);
        log.info("Deducted expense {} from user {} balance: newCash={}, newCard={}, totalAvailable={}",
                safeAmt, user.getId(), newCash, newCard, saved.getAvailableBalance());
        return saved;
    }

    private boolean isCard(String paymentMethod) {
        if (paymentMethod == null) return false;
        String lower = paymentMethod.trim().toLowerCase();
        return lower.contains("karta") || lower.contains("card") || lower.contains("plastik");
    }
}
