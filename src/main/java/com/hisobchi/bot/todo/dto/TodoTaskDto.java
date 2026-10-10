package com.hisobchi.bot.todo.dto;

import com.hisobchi.bot.todo.entity.TodoPriority;
import com.hisobchi.bot.todo.entity.TodoRecurrenceType;
import com.hisobchi.bot.todo.entity.TodoStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record TodoTaskDto(
        Long id,
        Long userId,
        Long projectId,
        String projectName,
        String title,
        String description,
        TodoStatus status,
        TodoPriority priority,
        LocalDate dueDate,
        LocalTime dueTime,
        boolean hasSpecificTime,
        Instant reminderAt,
        TodoRecurrenceType recurrenceType,
        BigDecimal plannedAmount,
        BigDecimal actualAmount,
        String currency,
        Long categoryId,
        String categoryName,
        Long linkedTransactionId,
        Long linkedDebtId,
        int totalSubtasks,
        int completedSubtasks,
        boolean isOverdue,
        Instant completedAt,
        Instant createdAt
) {
    public int getSubtaskProgressPercent() {
        if (totalSubtasks == 0) return 0;
        return (int) Math.round(((double) completedSubtasks / totalSubtasks) * 100);
    }
}
