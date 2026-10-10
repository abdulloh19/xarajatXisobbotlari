package com.hisobchi.bot.todo.dto;

import com.hisobchi.bot.todo.entity.TodoPriority;
import com.hisobchi.bot.todo.entity.TodoRecurrenceType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record ParsedTodo(
        String title,
        String description,
        LocalDate dueDate,
        LocalTime dueTime,
        boolean hasSpecificTime,
        Integer reminderMinutesBefore,
        TodoRecurrenceType recurrenceType,
        String recurrenceDaysOfWeek,
        Integer recurrenceIntervalDays,
        Integer recurrenceDayOfMonth,
        TodoPriority priority,
        String projectName,
        String categoryName,
        BigDecimal plannedAmount,
        String currency,
        boolean isExpenseIntent,
        boolean isAmbiguousTime,
        double confidence,
        String rawText
) {
    public boolean hasDate() {
        return dueDate != null;
    }

    public boolean hasAmount() {
        return plannedAmount != null && plannedAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
