package com.hisobchi.bot.profit.repository;

import com.hisobchi.bot.profit.entity.DailyProfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyProfitRepository extends JpaRepository<DailyProfit, Long> {

    Optional<DailyProfit> findByUserIdAndProfitDate(Long userId, LocalDate profitDate);

    boolean existsByUserIdAndProfitDate(Long userId, LocalDate profitDate);

    List<DailyProfit> findAllByUserIdAndProfitDateBetweenOrderByProfitDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<DailyProfit> findAllByUserIdAndProfitDateLessThanOrderByProfitDateDesc(Long userId, LocalDate date);
}
