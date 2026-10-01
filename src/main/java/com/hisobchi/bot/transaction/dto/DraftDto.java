package com.hisobchi.bot.transaction.dto;

import com.hisobchi.bot.transaction.entity.DraftStatus;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record DraftDto(
        Long id,
        Long userId,
        Long categoryId,
        String categoryName,
        String categoryEmoji,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        TransactionSource source,
        String originalText,
        Double confidence,
        DraftStatus status,
        Instant expiresAt,
        Instant createdAt
) {
    public String getCategoryDisplayName() {
        if (categoryName == null) return "Boshqa";
        return (categoryEmoji != null && !categoryEmoji.isBlank() ? categoryEmoji + " " : "") + categoryName;
    }
}
