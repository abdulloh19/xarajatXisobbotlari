package com.hisobchi.bot.todo.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record TodoDailyBriefDto(
        List<TodoTaskDto> todayTasks,
        List<TodoTaskDto> overdueTasks,
        List<TodoTaskDto> todayPlannedPayments,
        BigDecimal totalPlannedPaymentToday,
        List<TodoTaskDto> topRecommendedTasks
) {
}
