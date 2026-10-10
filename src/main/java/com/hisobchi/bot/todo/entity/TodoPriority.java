package com.hisobchi.bot.todo.entity;

public enum TodoPriority {
    LOW,
    MEDIUM,
    HIGH;

    public String getEmoji() {
        return switch (this) {
            case HIGH -> "🔴";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
        };
    }

    public String getDisplayName() {
        return switch (this) {
            case HIGH -> "Yuqori";
            case MEDIUM -> "Odatiy";
            case LOW -> "Past";
        };
    }
}
