package com.hisobchi.bot.todo.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProjectSummaryDto(
        Long id,
        String name,
        String description,
        String color,
        long totalTasks,
        long completedTasks,
        BigDecimal plannedAmount,
        BigDecimal actualAmount
) {
    public int getProgressPercent() {
        if (totalTasks == 0) return 0;
        return (int) Math.round(((double) completedTasks / totalTasks) * 100);
    }
}
