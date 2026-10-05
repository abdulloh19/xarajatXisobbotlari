package com.hisobchi.bot.profit.service;

import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.repository.DailyProfitRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyProfitService {

    private final DailyProfitRepository dailyProfitRepository;
    private final com.hisobchi.bot.user.service.BalanceService balanceService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public DailyProfit saveOrUpdateProfit(User user, LocalDate date, BigDecimal cash, BigDecimal card) {
        BigDecimal safeCash = cash != null ? cash : BigDecimal.ZERO;
        BigDecimal safeCard = card != null ? card : BigDecimal.ZERO;
        BigDecimal total = safeCash.add(safeCard);

        DailyProfit record = dailyProfitRepository.findByUserIdAndProfitDate(user.getId(), date)
                .orElseGet(() -> DailyProfit.builder()
                        .user(user)
                        .profitDate(date)
                        .build());

        BigDecimal oldCash = record.getCashAmount() != null ? record.getCashAmount() : BigDecimal.ZERO;
        BigDecimal oldCard = record.getCardAmount() != null ? record.getCardAmount() : BigDecimal.ZERO;
        BigDecimal deltaCash = safeCash.subtract(oldCash);
        BigDecimal deltaCard = safeCard.subtract(oldCard);

        record.setWorkDay(true);
        record.setCashAmount(safeCash);
        record.setCardAmount(safeCard);
        record.setTotalProfit(total);

        DailyProfit saved = dailyProfitRepository.save(record);
        balanceService.updateDailyProfitDelta(user, deltaCash, deltaCard);
        log.info("Saved daily profit for user {} on {}: cash={}, card={}, total={}",
                user.getId(), date, safeCash, safeCard, total);
        return saved;
    }

    @Transactional
    public DailyProfit markOffDay(User user, LocalDate date) {
        DailyProfit record = dailyProfitRepository.findByUserIdAndProfitDate(user.getId(), date)
                .orElseGet(() -> DailyProfit.builder()
                        .user(user)
                        .profitDate(date)
                        .build());

        BigDecimal oldCash = record.getCashAmount() != null ? record.getCashAmount() : BigDecimal.ZERO;
        BigDecimal oldCard = record.getCardAmount() != null ? record.getCardAmount() : BigDecimal.ZERO;
        BigDecimal deltaCash = BigDecimal.ZERO.subtract(oldCash);
        BigDecimal deltaCard = BigDecimal.ZERO.subtract(oldCard);

        record.setWorkDay(false);
        record.setCashAmount(BigDecimal.ZERO);
        record.setCardAmount(BigDecimal.ZERO);
        record.setTotalProfit(BigDecimal.ZERO);

        DailyProfit saved = dailyProfitRepository.save(record);
        balanceService.updateDailyProfitDelta(user, deltaCash, deltaCard);
        log.info("Marked day {} as OFF DAY (dam olish) for user {}", date, user.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isOffDay(Long userId, LocalDate date) {
        Optional<DailyProfit> p = dailyProfitRepository.findByUserIdAndProfitDate(userId, date);
        return p.isPresent() && !p.get().isWorkDay();
    }

    @Transactional(readOnly = true)
    public Optional<DailyProfit> getLastWorkedDayProfit(Long userId, LocalDate beforeDate) {
        List<DailyProfit> list = dailyProfitRepository.findAllByUserIdAndProfitDateLessThanOrderByProfitDateDesc(userId, beforeDate);
        for (DailyProfit p : list) {
            if (p.isWorkDay() && p.getTotalProfit() != null && p.getTotalProfit().compareTo(BigDecimal.ZERO) > 0) {
                return Optional.of(p);
            }
        }
        for (DailyProfit p : list) {
            if (p.isWorkDay()) {
                return Optional.of(p);
            }
        }
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Transactional(readOnly = true)
    public Optional<DailyProfit> getProfit(Long userId, LocalDate date) {
        return dailyProfitRepository.findByUserIdAndProfitDate(userId, date);
    }

    @Transactional(readOnly = true)
    public boolean hasProfit(Long userId, LocalDate date) {
        return dailyProfitRepository.existsByUserIdAndProfitDate(userId, date);
    }

    @Transactional(readOnly = true)
    public List<DailyProfit> getProfitsBetween(Long userId, LocalDate start, LocalDate end) {
        return dailyProfitRepository.findAllByUserIdAndProfitDateBetweenOrderByProfitDateAsc(userId, start, end);
    }

    @Transactional
    public void deductFromProfit(User user, LocalDate date, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Optional<DailyProfit> profitOpt = dailyProfitRepository.findByUserIdAndProfitDate(user.getId(), date);
        DailyProfit p;
        if (profitOpt.isPresent()) {
            p = profitOpt.get();
        } else {
            BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, date);
            p = DailyProfit.builder()
                    .user(user)
                    .profitDate(date)
                    .cashAmount(inc != null ? inc : BigDecimal.ZERO)
                    .cardAmount(BigDecimal.ZERO)
                    .isWorkDay(true)
                    .build();
        }
        BigDecimal currentCash = p.getCashAmount() != null ? p.getCashAmount() : BigDecimal.ZERO;
        BigDecimal currentCard = p.getCardAmount() != null ? p.getCardAmount() : BigDecimal.ZERO;

        BigDecimal newCash = currentCash.subtract(amount);
        BigDecimal newCard = currentCard;
        if (newCash.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal deficit = newCash.abs();
            newCash = BigDecimal.ZERO;
            newCard = currentCard.subtract(deficit);
            if (newCard.compareTo(BigDecimal.ZERO) < 0) {
                newCard = BigDecimal.ZERO;
            }
        }
        saveOrUpdateProfit(user, date, newCash, newCard);
        log.info("Deducted {} from profit for user {} on {}: new total={}",
                amount, user.getId(), date, newCash.add(newCard));
    }

    @Transactional
    public DailyProfit addToProfit(User user, LocalDate date, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return getProfit(user.getId(), date).orElse(null);
        }
        DailyProfit p = dailyProfitRepository.findByUserIdAndProfitDate(user.getId(), date)
                .orElseGet(() -> DailyProfit.builder()
                        .user(user)
                        .profitDate(date)
                        .isWorkDay(true)
                        .build());
        BigDecimal currentCash = p.getCashAmount() != null ? p.getCashAmount() : BigDecimal.ZERO;
        BigDecimal currentCard = p.getCardAmount() != null ? p.getCardAmount() : BigDecimal.ZERO;

        BigDecimal newCash = currentCash.add(amount);
        DailyProfit updated = saveOrUpdateProfit(user, date, newCash, currentCard);
        log.info("Added {} to profit for user {} on {}: new total={}", amount, user.getId(), date, updated.getTotalProfit());
        return updated;
    }

    @Transactional
    public DailyProfit subtractFromProfit(User user, LocalDate date, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return getProfit(user.getId(), date).orElse(null);
        }
        DailyProfit p = dailyProfitRepository.findByUserIdAndProfitDate(user.getId(), date)
                .orElseGet(() -> {
                    BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, date);
                    return DailyProfit.builder()
                            .user(user)
                            .profitDate(date)
                            .cashAmount(inc != null ? inc : BigDecimal.ZERO)
                            .cardAmount(BigDecimal.ZERO)
                            .isWorkDay(true)
                            .build();
                });
        BigDecimal currentCash = p.getCashAmount() != null ? p.getCashAmount() : BigDecimal.ZERO;
        BigDecimal currentCard = p.getCardAmount() != null ? p.getCardAmount() : BigDecimal.ZERO;

        BigDecimal newCash = currentCash.subtract(amount);
        BigDecimal newCard = currentCard;
        if (newCash.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal deficit = newCash.abs();
            newCash = BigDecimal.ZERO;
            newCard = currentCard.subtract(deficit);
            if (newCard.compareTo(BigDecimal.ZERO) < 0) {
                newCard = BigDecimal.ZERO;
            }
        }
        DailyProfit updated = saveOrUpdateProfit(user, date, newCash, newCard);
        log.info("Subtracted {} from profit for user {} on {}: new total={}", amount, user.getId(), date, updated.getTotalProfit());
        return updated;
    }

    @Transactional(readOnly = true)
    public boolean hasEnteredProfitToday(User user, LocalDate today) {
        Optional<DailyProfit> todayProfitOpt = dailyProfitRepository.findByUserIdAndProfitDate(user.getId(), today);
        if (todayProfitOpt.isPresent()
                && todayProfitOpt.get().isWorkDay()
                && todayProfitOpt.get().getTotalProfit() != null
                && todayProfitOpt.get().getTotalProfit().compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }
        BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, today);
        return inc != null && inc.compareTo(BigDecimal.ZERO) > 0;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTodayProfitOrIncome(User user, LocalDate today) {
        Optional<DailyProfit> todayProfitOpt = dailyProfitRepository.findByUserIdAndProfitDate(user.getId(), today);
        if (todayProfitOpt.isPresent()
                && todayProfitOpt.get().isWorkDay()
                && todayProfitOpt.get().getTotalProfit() != null) {
            return todayProfitOpt.get().getTotalProfit();
        }
        BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDate(user.getId(), TransactionType.INCOME, today);
        return inc != null ? inc : BigDecimal.ZERO;
    }
}
