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

        record.setCashAmount(safeCash);
        record.setCardAmount(safeCard);
        record.setTotalProfit(total);

        DailyProfit saved = dailyProfitRepository.save(record);
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
}
