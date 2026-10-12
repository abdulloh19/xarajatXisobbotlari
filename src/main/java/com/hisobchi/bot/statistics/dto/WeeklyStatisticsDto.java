package com.hisobchi.bot.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyStatisticsDto(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit,
        long transactionCount,
        List<CategoryExpenseDto> expenseCategories,
        CategoryExpenseDto topExpenseCategory,
        List<CategoryExpenseDto> incomeCategories
) {
    public WeeklyStatisticsDto(LocalDate startDate, LocalDate endDate, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netProfit, long transactionCount, List<CategoryExpenseDto> expenseCategories, CategoryExpenseDto topExpenseCategory) {
        this(startDate, endDate, totalIncome, totalExpense, netProfit, transactionCount, expenseCategories, topExpenseCategory, List.of());
    }
}
