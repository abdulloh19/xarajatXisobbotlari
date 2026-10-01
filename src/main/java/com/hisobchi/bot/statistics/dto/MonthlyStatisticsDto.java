package com.hisobchi.bot.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyStatisticsDto(
        LocalDate monthDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit,
        long activeDays,
        BigDecimal averageDailyIncome,
        BigDecimal averageDailyExpense,
        BigDecimal averageDailyNetProfit,
        List<CategoryExpenseDto> expenseCategories
) {}
