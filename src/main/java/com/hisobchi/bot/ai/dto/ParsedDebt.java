package com.hisobchi.bot.ai.dto;

import com.hisobchi.bot.debt.entity.DebtType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedDebt(
        DebtType type,
        BigDecimal amount,
        String personName,
        LocalDate dueDate,
        String description,
        double confidence,
        String rawText,
        String paymentMethod
) {
    public ParsedDebt(
            DebtType type,
            BigDecimal amount,
            String personName,
            LocalDate dueDate,
            String description,
            double confidence,
            String rawText
    ) {
        this(type, amount, personName, dueDate, description, confidence, rawText, "Naqd");
    }
}
