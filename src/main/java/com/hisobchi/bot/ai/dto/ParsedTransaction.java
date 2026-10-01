package com.hisobchi.bot.ai.dto;

import com.hisobchi.bot.transaction.entity.TransactionType;

import java.math.BigDecimal;

public record ParsedTransaction(
        TransactionType type,
        BigDecimal amount,
        String category,
        String description,
        double confidence,
        String originalText
) {}
