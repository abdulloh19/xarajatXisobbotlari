package com.hisobchi.bot.summary.service;

import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.summary.entity.DailySummary;
import com.hisobchi.bot.summary.repository.DailySummaryRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final DailySummaryRepository dailySummaryRepository;

    @Transactional(readOnly = true)
    public boolean isDayClosed(Long userId, LocalDate date) {
        return dailySummaryRepository.existsByUserIdAndSummaryDateAndClosedTrue(userId, date);
    }

    @Transactional(readOnly = true)
    public Optional<DailySummary> getSummary(Long userId, LocalDate date) {
        return dailySummaryRepository.findByUserIdAndSummaryDate(userId, date);
    }

    @Transactional
    public DailySummary closeDay(User user, LocalDate date, BigDecimal totalEarned, BigDecimal expense, BigDecimal netProfit) {
        BigDecimal safeEarned = totalEarned != null ? totalEarned : BigDecimal.ZERO;
        BigDecimal safeExpense = expense != null ? expense : BigDecimal.ZERO;
        BigDecimal safeProfit = netProfit != null ? netProfit : BigDecimal.ZERO;

        DailySummary summary = dailySummaryRepository.findByUserIdAndSummaryDate(user.getId(), date)
                .orElseGet(() -> DailySummary.builder()
                        .user(user)
                        .summaryDate(date)
                        .build());

        summary.setTotalIncome(safeEarned);
        summary.setTotalExpense(safeExpense);
        summary.setNetProfit(safeProfit);
        summary.setClosed(true);
        summary.setClosedAt(Instant.now());

        log.info("Closed day {} for user id {}: earned={}, expense={}, profit={}",
                date, user.getId(), safeEarned, safeExpense, safeProfit);
        return dailySummaryRepository.save(summary);
    }

    @Transactional
    public DailySummary closeDay(User user, LocalDate date, BigDecimal totalEarned, BigDecimal expense) {
        BigDecimal safeEarned = totalEarned != null ? totalEarned : BigDecimal.ZERO;
        BigDecimal safeExpense = expense != null ? expense : BigDecimal.ZERO;
        BigDecimal safeProfit = safeEarned.compareTo(safeExpense) >= 0 ? safeEarned.subtract(safeExpense) : safeEarned;
        return closeDay(user, date, safeEarned, safeExpense, safeProfit);
    }

    @Transactional
    public DailySummary reopenDay(Long userId, LocalDate date) {
        DailySummary summary = dailySummaryRepository.findByUserIdAndSummaryDate(userId, date)
                .orElseThrow(() -> new EntityNotFoundException("Bu sana bo‘yicha yopilgan kun topilmadi: " + date));

        summary.setClosed(false);
        log.info("Reopened day {} for user id {}", date, userId);
        return dailySummaryRepository.save(summary);
    }
}
