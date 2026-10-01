package com.hisobchi.bot.ai.dto;

public record TranscriptionResult(
        String text,
        boolean success,
        String errorMessage
) {
    public static TranscriptionResult success(String text) {
        return new TranscriptionResult(text, true, null);
    }

    public static TranscriptionResult failure(String errorMessage) {
        return new TranscriptionResult("", false, errorMessage);
    }
}
