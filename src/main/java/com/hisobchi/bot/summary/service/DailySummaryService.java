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
    public DailySummary closeDay(User user, LocalDate date, BigDecimal income, BigDecimal expense) {
        BigDecimal totalIncome = income != null ? income : BigDecimal.ZERO;
        BigDecimal totalExpense = expense != null ? expense : BigDecimal.ZERO;
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        DailySummary summary = dailySummaryRepository.findByUserIdAndSummaryDate(user.getId(), date)
                .orElseGet(() -> DailySummary.builder()
                        .user(user)
                        .summaryDate(date)
                        .build());

        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setNetProfit(netProfit);
        summary.setClosed(true);
        summary.setClosedAt(Instant.now());

        log.info("Closed day {} for user id {}: income={}, expense={}, profit={}",
                date, user.getId(), totalIncome, totalExpense, netProfit);
        return dailySummaryRepository.save(summary);
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
