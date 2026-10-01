package com.hisobchi.bot.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final String DEFAULT_TIMEZONE = "Asia/Tashkent";
    public static final ZoneId DEFAULT_ZONE = ZoneId.of(DEFAULT_TIMEZONE);
    public static final java.util.Locale UZBEK_LOCALE = java.util.Locale.forLanguageTag("uz-UZ");

    public static ZoneId getZoneId(String timezone) {
        return resolveZone(timezone);
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] UZBEK_MONTHS = {
            "yanvar", "fevral", "mart", "aprel", "may", "iyun",
            "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"
    };

    private DateTimeUtils() {
    }

    public static ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            return DEFAULT_ZONE;
        }
    }

    public static LocalDate today(String timezone) {
        return LocalDate.now(resolveZone(timezone));
    }

    public static ZonedDateTime now(String timezone) {
        return ZonedDateTime.now(resolveZone(timezone));
    }

    public static String formatDateTime(Instant instant, String timezone) {
        if (instant == null) {
            return "";
        }
        return DATE_TIME_FORMATTER.format(instant.atZone(resolveZone(timezone)));
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMATTER.format(date);
    }

    public static String formatTime(Instant instant, String timezone) {
        if (instant == null) {
            return "";
        }
        return TIME_FORMATTER.format(instant.atZone(resolveZone(timezone)));
    }

    public static String formatUzbekDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        int day = date.getDayOfMonth();
        String month = UZBEK_MONTHS[date.getMonthValue() - 1];
        int year = date.getYear();
        return day + "-" + month + " " + year;
    }

    public static String formatUzbekMonthYear(LocalDate date) {
        if (date == null) {
            return "";
        }
        String month = UZBEK_MONTHS[date.getMonthValue() - 1].toUpperCase();
        return month + " " + date.getYear();
    }

    public static String formatUzbekDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        String startMonth = UZBEK_MONTHS[start.getMonthValue() - 1];
        String endMonth = UZBEK_MONTHS[end.getMonthValue() - 1];

        if (start.getMonthValue() == end.getMonthValue() && start.getYear() == end.getYear()) {
            return start.getDayOfMonth() + "-" + end.getDayOfMonth() + " " + endMonth + " " + end.getYear();
        }
        return start.getDayOfMonth() + "-" + startMonth + " — " + end.getDayOfMonth() + "-" + endMonth + " " + end.getYear();
    }
}
