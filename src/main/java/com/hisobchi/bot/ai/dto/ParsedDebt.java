package com.hisobchi.bot.ai.dto;

import com.hisobchi.bot.debt.entity.DebtType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedDebt(
        DebtIntent intent,
        DebtType type,
        BigDecimal amount,
        String personName,
        LocalDate dueDate,
        String description,
        double confidence,
        String rawText,
        String paymentMethod,
        boolean missingPerson,
        boolean missingDueDate,
        boolean missingPaymentMethod
) {
    public ParsedDebt(
            DebtType type,
            BigDecimal amount,
            String personName,
            LocalDate dueDate,
            String description,
            double confidence,
            String rawText,
            String paymentMethod
    ) {
        this(
                type == DebtType.BORROWED ? DebtIntent.BORROW : DebtIntent.LEND,
                type,
                amount,
                personName,
                dueDate,
                description,
                confidence,
                rawText,
                paymentMethod,
                personName == null || personName.isBlank() || "Noma'lum".equalsIgnoreCase(personName),
                dueDate == null,
                paymentMethod == null
        );
    }

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
