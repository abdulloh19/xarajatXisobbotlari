package com.hisobchi.bot.debt.dto;

import java.math.BigDecimal;

public record DebtStatisticsDto(
        BigDecimal borrowedOriginal,
        BigDecimal borrowedPaid,
        BigDecimal borrowedRemaining,
        BigDecimal lentOriginal,
        BigDecimal lentPaid,
        BigDecimal lentRemaining,
        long activeBorrowedCount,
        long activeLentCount,
        long overdueCount
) {
    // Backward compatibility constructor
    public DebtStatisticsDto(
            BigDecimal totalToPay,
            BigDecimal totalToReceive,
            long activeBorrowedCount,
            long activeLentCount,
            long overdueCount
    ) {
        this(
                totalToPay, BigDecimal.ZERO, totalToPay,
                totalToReceive, BigDecimal.ZERO, totalToReceive,
                activeBorrowedCount, activeLentCount, overdueCount
        );
    }

    public BigDecimal totalToPay() {
        return borrowedRemaining != null ? borrowedRemaining : BigDecimal.ZERO;
    }

    public BigDecimal totalToReceive() {
        return lentRemaining != null ? lentRemaining : BigDecimal.ZERO;
    }
}
