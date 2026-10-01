package com.hisobchi.bot.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class UzbekDateParser {

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("yanvar", 1), Map.entry("yanvargacha", 1),
            Map.entry("fevral", 2), Map.entry("fevralgacha", 2),
            Map.entry("mart", 3), Map.entry("martgacha", 3),
            Map.entry("aprel", 4), Map.entry("aprelgacha", 4),
            Map.entry("may", 5), Map.entry("maygacha", 5),
            Map.entry("iyun", 6), Map.entry("iyungacha", 6),
            Map.entry("iyul", 7), Map.entry("iyulgacha", 7),
            Map.entry("avgust", 8), Map.entry("avgustgacha", 8),
            Map.entry("sentabr", 9), Map.entry("sentyabr", 9), Map.entry("sentabrgacha", 9), Map.entry("sentyabrgacha", 9),
            Map.entry("oktabr", 10), Map.entry("oktyabr", 10), Map.entry("oktabrgacha", 10), Map.entry("oktyabrgacha", 10),
            Map.entry("noyabr", 11), Map.entry("noyabrgacha", 11),
            Map.entry("dekabr", 12), Map.entry("dekabrgacha", 12)
    );

    private static final Map<String, DayOfWeek> DAYS_OF_WEEK = Map.of(
            "dushanba", DayOfWeek.MONDAY,
            "seshanba", DayOfWeek.TUESDAY,
            "chorshanba", DayOfWeek.WEDNESDAY,
            "payshanba", DayOfWeek.THURSDAY,
            "juma", DayOfWeek.FRIDAY,
            "shanba", DayOfWeek.SATURDAY,
            "yakshanba", DayOfWeek.SUNDAY
    );

    public Optional<LocalDate> parseDate(String text, ZoneId zoneId) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        LocalDate today = LocalDate.now(zoneId);
        String lower = text.toLowerCase().trim();

        // 1. "ertaga"
        if (lower.contains("ertaga")) {
            return Optional.of(today.plusDays(1));
        }

        // 2. "indin"
        if (lower.contains("indin")) {
            return Optional.of(today.plusDays(2));
        }

        // 3. "X kundan keyin", "X kun ichida", "X kunda"
        Matcher daysAfterMatcher = Pattern.compile("(\\d+)\\s*(?:kundan\\s*keyin|kun\\s*ichida|kunda)").matcher(lower);
        if (daysAfterMatcher.find()) {
            int days = Integer.parseInt(daysAfterMatcher.group(1));
            return Optional.of(today.plusDays(days));
        }

        // 4. "X haftadan keyin", "ikki haftadan keyin", "uch haftadan keyin", "bir haftadan keyin"
        if (lower.contains("ikki haftadan keyin") || lower.contains("2 haftadan keyin")) {
            return Optional.of(today.plusWeeks(2));
        }
        if (lower.contains("uch haftadan keyin") || lower.contains("3 haftadan keyin")) {
            return Optional.of(today.plusWeeks(3));
        }
        Matcher weeksAfterMatcher = Pattern.compile("(\\d+)\\s*haftadan\\s*keyin").matcher(lower);
        if (weeksAfterMatcher.find()) {
            int weeks = Integer.parseInt(weeksAfterMatcher.group(1));
            return Optional.of(today.plusWeeks(weeks));
        }
        if (lower.contains("keyingi hafta") || lower.contains("bir haftadan keyin") || lower.contains("1 haftadan keyin")) {
            return Optional.of(today.plusWeeks(1));
        }

        // 4b. "bir oydan keyin", "1 oydan keyin", "X oydan keyin"
        if (lower.contains("bir oydan keyin") || lower.contains("1 oydan keyin")) {
            return Optional.of(today.plusMonths(1));
        }
        Matcher monthsAfterMatcher = Pattern.compile("(\\d+)\\s*oydan\\s*keyin").matcher(lower);
        if (monthsAfterMatcher.find()) {
            int months = Integer.parseInt(monthsAfterMatcher.group(1));
            return Optional.of(today.plusMonths(months));
        }

        // 5. "keyingi <kun>" e.g. "keyingi dushanba", "keyingi juma"
        for (Map.Entry<String, DayOfWeek> entry : DAYS_OF_WEEK.entrySet()) {
            if (lower.contains("keyingi " + entry.getKey())) {
                LocalDate next = today.with(TemporalAdjusters.next(entry.getValue()));
                return Optional.of(next);
            }
        }

        // 6. "<kun> kuni" e.g. "juma kuni", "dushanba kuni"
        for (Map.Entry<String, DayOfWeek> entry : DAYS_OF_WEEK.entrySet()) {
            if (lower.contains(entry.getKey() + " kuni") || lower.contains(entry.getKey() + "gacha")) {
                LocalDate next = today.with(TemporalAdjusters.nextOrSame(entry.getValue()));
                if (next.equals(today)) {
                    next = next.plusWeeks(1);
                }
                return Optional.of(next);
            }
        }

        // 7. "oy oxirida", "oy oxirigacha"
        if (lower.contains("oy oxirida") || lower.contains("oy oxirigacha") || lower.contains("oy oxiri")) {
            return Optional.of(today.with(TemporalAdjusters.lastDayOfMonth()));
        }

        // 8. "10-chigacha", "10 chigacha", "15-gacha"
        Matcher dayOfMonthMatcher = Pattern.compile("(\\d{1,2})\\s*-(?:chi|chigacha|gacha|i)?|(\\d{1,2})\\s*chigacha").matcher(lower);
        if (dayOfMonthMatcher.find()) {
            String dayStr = dayOfMonthMatcher.group(1) != null ? dayOfMonthMatcher.group(1) : dayOfMonthMatcher.group(2);
            int day = Integer.parseInt(dayStr);
            if (day >= 1 && day <= 31) {
                try {
                    LocalDate target = today.withDayOfMonth(day);
                    if (target.isBefore(today)) {
                        target = today.plusMonths(1).withDayOfMonth(day);
                    }
                    return Optional.of(target);
                } catch (Exception ignored) {
                }
            }
        }

        // 9. "15 oktabr", "15 oktyabrgacha"
        Matcher monthNameMatcher = Pattern.compile("(\\d{1,2})\\s*([a-z]+)").matcher(lower);
        while (monthNameMatcher.find()) {
            int day = Integer.parseInt(monthNameMatcher.group(1));
            String monthName = monthNameMatcher.group(2);
            if (MONTHS.containsKey(monthName)) {
                int month = MONTHS.get(monthName);
                int year = today.getYear();
                try {
                    LocalDate target = LocalDate.of(year, month, day);
                    if (target.isBefore(today)) {
                        target = target.plusYears(1);
                    }
                    return Optional.of(target);
                } catch (Exception ignored) {
                }
            }
        }

        // 10. "15.10", "15.10.2026", "15/10/2026"
        Matcher numericDateMatcher = Pattern.compile("(\\d{1,2})[./](\\d{1,2})(?:[./](\\d{4}))?").matcher(lower);
        if (numericDateMatcher.find()) {
            int day = Integer.parseInt(numericDateMatcher.group(1));
            int month = Integer.parseInt(numericDateMatcher.group(2));
            int year = numericDateMatcher.group(3) != null ? Integer.parseInt(numericDateMatcher.group(3)) : today.getYear();
            try {
                LocalDate target = LocalDate.of(year, month, day);
                if (target.isBefore(today) && numericDateMatcher.group(3) == null) {
                    target = target.plusYears(1);
                }
                return Optional.of(target);
            } catch (Exception ignored) {
            }
        }

        return Optional.empty();
    }
}
