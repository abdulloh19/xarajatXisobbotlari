package com.hisobchi.bot.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyStatisticsDto(
        LocalDate monthDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit,
        BigDecimal cashProfit,
        BigDecimal cardProfit,
        long activeDays,
        BigDecimal averageDailyIncome,
        BigDecimal averageDailyExpense,
        BigDecimal averageDailyNetProfit,
        List<CategoryExpenseDto> expenseCategories
) {
    public MonthlyStatisticsDto(
            LocalDate monthDate,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netProfit,
            long activeDays,
            BigDecimal averageDailyIncome,
            BigDecimal averageDailyExpense,
            BigDecimal averageDailyNetProfit,
            List<CategoryExpenseDto> expenseCategories) {
        this(monthDate, totalIncome, totalExpense, netProfit, BigDecimal.ZERO, BigDecimal.ZERO,
                activeDays, averageDailyIncome, averageDailyExpense, averageDailyNetProfit, expenseCategories);
    }
}
