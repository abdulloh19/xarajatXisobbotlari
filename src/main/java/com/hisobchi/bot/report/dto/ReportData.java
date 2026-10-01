package com.hisobchi.bot.report.dto;

import com.hisobchi.bot.statistics.dto.CategoryExpenseDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record ReportData(
        String title,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal totalEarned,
        BigDecimal totalExpense,
        BigDecimal totalProfit,
        BigDecimal cashProfit,
        BigDecimal cardProfit,
        long activeDays,
        long completedDays,
        long incompleteDays,
        List<LocalDate> incompleteDates,
        List<CategoryExpenseDto> categoryExpenses,
        boolean profitEntered
) {}
