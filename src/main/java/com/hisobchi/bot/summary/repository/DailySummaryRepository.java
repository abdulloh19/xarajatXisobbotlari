package com.hisobchi.bot.summary.repository;

import com.hisobchi.bot.summary.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    Optional<DailySummary> findByUserIdAndSummaryDate(Long userId, LocalDate summaryDate);

    boolean existsByUserIdAndSummaryDateAndClosedTrue(Long userId, LocalDate summaryDate);
}
