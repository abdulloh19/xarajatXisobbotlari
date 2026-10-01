package com.hisobchi.bot.debt.dto;

import java.math.BigDecimal;

public record DebtStatisticsDto(
        BigDecimal totalToPay,       // BORROWED active sum
        BigDecimal totalToReceive,   // LENT active sum
        long activeBorrowedCount,
        long activeLentCount,
        long overdueCount
) {}
