package com.hisobchi.bot.common.exception;

public class HisobchiException extends RuntimeException {
    public HisobchiException(String message) {
        super(message);
    }

    public HisobchiException(String message, Throwable cause) {
        super(message, cause);
    }
}
