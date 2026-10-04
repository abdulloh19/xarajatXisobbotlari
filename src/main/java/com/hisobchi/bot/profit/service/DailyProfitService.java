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

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyProfitService {

    private final DailyProfitRepository dailyProfitRepository;
    private final com.hisobchi.bot.user.service.BalanceService balanceService;

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

        record.setCashAmount(safeCash);
        record.setCardAmount(safeCard);
        record.setTotalProfit(total);

        DailyProfit saved = dailyProfitRepository.save(record);
        balanceService.updateDailyProfitDelta(user, deltaCash, deltaCard);
        log.info("Saved daily profit for user {} on {}: cash={}, card={}, total={}",
                user.getId(), date, safeCash, safeCard, total);
        return saved;
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
        if (profitOpt.isPresent()) {
            DailyProfit p = profitOpt.get();
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
    }
}
