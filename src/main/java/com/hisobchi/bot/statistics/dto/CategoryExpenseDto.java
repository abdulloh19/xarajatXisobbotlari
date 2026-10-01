package com.hisobchi.bot.statistics.dto;

import java.math.BigDecimal;

public record CategoryExpenseDto(
        Long categoryId,
        String categoryName,
        String categoryEmoji,
        BigDecimal totalAmount,
        Long transactionCount
) {
    public String getDisplayName() {
        if (categoryName == null) return "Boshqa";
        return (categoryEmoji != null && !categoryEmoji.isBlank() ? categoryEmoji + " " : "") + categoryName;
    }
}
