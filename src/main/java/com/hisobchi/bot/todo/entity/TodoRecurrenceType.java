package com.hisobchi.bot.todo.entity;

public enum TodoRecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    EVERY_N_DAYS;

    public String getDisplayName() {
        return switch (this) {
            case NONE -> "Bir martalik";
            case DAILY -> "Har kuni";
            case WEEKLY -> "Har hafta";
            case MONTHLY -> "Har oy";
            case EVERY_N_DAYS -> "Har N kunda";
        };
    }
}
