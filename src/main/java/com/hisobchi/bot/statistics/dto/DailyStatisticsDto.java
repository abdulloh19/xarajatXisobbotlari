package com.hisobchi.bot.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyStatisticsDto(
        LocalDate date,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit,
        long transactionCount,
        List<CategoryExpenseDto> expenseCategories,
        boolean isClosed,
        boolean isOffDay
) {
    public DailyStatisticsDto(LocalDate date, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netProfit, long transactionCount, List<CategoryExpenseDto> expenseCategories, boolean isClosed) {
        this(date, totalIncome, totalExpense, netProfit, transactionCount, expenseCategories, isClosed, false);
    }
}
