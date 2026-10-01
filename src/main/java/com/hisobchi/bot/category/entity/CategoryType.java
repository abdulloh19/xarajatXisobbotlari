package com.hisobchi.bot.category.entity;

import com.hisobchi.bot.transaction.entity.TransactionType;

public enum CategoryType {
    EXPENSE,
    INCOME;

    public static CategoryType from(TransactionType type) {
        if (type == null) return EXPENSE;
        return CategoryType.valueOf(type.name());
    }
}
