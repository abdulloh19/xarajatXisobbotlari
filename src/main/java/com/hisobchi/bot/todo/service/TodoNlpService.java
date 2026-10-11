package com.hisobchi.bot.todo.service;

import com.hisobchi.bot.ai.service.CategoryMatcher;
import com.hisobchi.bot.ai.service.UzbekAmountParser;
import com.hisobchi.bot.common.util.UzbekDateParser;
import com.hisobchi.bot.todo.dto.ParsedTodo;
import com.hisobchi.bot.todo.entity.TodoPriority;
import com.hisobchi.bot.todo.entity.TodoRecurrenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoNlpService {

    private final UzbekAmountParser amountParser;
    private final UzbekDateParser dateParser;
    private final CategoryMatcher categoryMatcher;

    // Past-tense expense indicators (meaning money was ALREADY spent)
    private static final Pattern PAST_EXPENSE_PATTERN = Pattern.compile(
            "\\b(?:to['‘`]?ladim|to['‘`]?lab\\s*bo['‘`]?ldim|berdim|sotib\\s*oldim|sarfladim|ishlatdim|ketdi|to['‘`]?landi|oplata\\s*qildim|yozib\\s*qo['‘`]?y|xarajat\\s*qildim)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Future task / reminder indicators (intention, to-do, reminder)
    private static final Pattern TASK_TRIGGER_PATTERN = Pattern.compile(
            "\\b(?:eslat|eslatgin|eslatma|eslatvor|eslatib\\s*qo['‘`]?y|napomni|напомни|напомнить|" +
            "to['‘`]?lashim\\s*(?:kerak|kere|kerag)|to['‘`]?lash\\s*(?:kerak|kere|kerag)|to['‘`]?layman|to['‘`]?lash|" +
            "qilishim\\s*(?:kerak|kere|kerag)|qilish\\s*(?:kerak|kere|kerag)|qilish|qilay|bajarish\\s*(?:kerak|kere|kerag)|bajarishim\\s*(?:kerak|kere|kerag)|bajarish|" +
            "olishim\\s*(?:kerak|kere|kerag)|olish\\s*(?:kerak|kere|kerag)|sotib\\s*olish\\s*(?:kerak|kere|kerag)|olish|" +
            "borishim\\s*(?:kerak|kere|kerag)|borish\\s*(?:kerak|kere|kerag)|borish|" +
            "telefon\\s*qilish|tel\\s*qilish|qo['‘`]?ng['‘`]?roq\\s*qilish|qo['‘`]?ng['‘`]?roq|" +
            "uchrashuv|uchrashish|gaplashish|ko['‘`]?rishish|" +
            "miting|majlis|sobranie|собрание|dars|darsim|repetitor|kurs|trening|mashg['‘`]?ulot|" +
            "topshirish\\s*(?:kerak|kere|kerag)|topshirish|tayyorlash\\s*(?:kerak|kere|kerag)|tayyorlash|" +
            "vazifa|zadacha|reja|har\\s*oyning|har\\s*kuni|har\\s*hafta)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Event or activity pattern
    private static final Pattern EVENT_OR_ACTION_PATTERN = Pattern.compile(
            "\\b(?:miting|majlis|sobranie|собрание|dars|darsim|kurs|trening|mashg['‘`]?ulot|mashq|trenirovka|" +
            "o['‘`]?yin|oyin|futbol|match|zal|fitnes|bassen|tennis|yugurish|" +
            "uchrashuv|vstrecha|встреча|intervyu|suhbat|doktor|vrach|shifokor|konsultatsiya|" +
            "telefon|tel|qo['‘`]?ng['‘`]?roq|zvonok|звонок|" +
            "choyxona|gap|to['‘`]?y|ziyofat|tadbir|marosim|imtihon|test|sessiya|konsert|kino|kinoga|mehmon|" +
            "bor\\b|bo['‘`]?ladi|boladi|boshlanadi|bo['‘`]?lib\\s*o['‘`]?tadi|" +
            "borish|kelish|ketish|uchish|qilish|yozish|o['‘`]?qish|topshirish|ko['‘`]?rishish|" +
            "kere\\b|kerak\\b|kerag\\b)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Time or relative date indicators
    private static final Pattern TIME_OR_DATE_PATTERN = Pattern.compile(
            "\\b(?:soat(?:i|iga|ida)?\\s*\\d{1,2}|\\d{1,2}[:.]\\d{2}|\\d{1,2}(?:[-–\\s]*(?:da|ga|dagi|gacha))|" +
            "bugun|ertaga|indinga|kechqurun|ertalab|saharda|tushda|" +
            "dushanba|seshanba|chorshanba|payshanba|juma|shanba|yakshanba|" +
            "завтра|сегодня|послезавтра)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Explicit time pattern: "18:00", "09.30", "15:00 da", "soat 18:00 da", "12:00-da"
    private static final Pattern TIME_EXPLICIT_PATTERN = Pattern.compile(
            "(?:soat(?:i|iga|ida)?\\s*)?(\\d{1,2})[:.](\\d{2})(?:[-–\\s]*(?:da|ga|dagi|gacha))?",
            Pattern.CASE_INSENSITIVE
    );

    // Relative hour pattern: "soat 9 da", "soati 7da", "9 da", "soat 12-da", "12-da", "18 da", "ertalab 9 da", "kechki 6 da", "soat 9 ga", "soat 9-da"
    private static final Pattern HOUR_ONLY_PATTERN = Pattern.compile(
            "(?:(?:ertalab|saharda|kunduzi|tushda|kechki|kechqurun|oqshom)\\s+)?(?:soat(?:i|iga|ida)?\\s*)?(\\d{1,2})(?:[-–\\s]*(?:da|ga|dagi|gacha))\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Explicit financial intent or currency/multiplier pattern for tasks
    private static final Pattern FINANCIAL_OR_AMOUNT_PATTERN = Pattern.compile(
            "\\b(?:to['‘`]?lov|to['‘`]?lash|pul|qarz|nasiya|ijara|kredit|rassrochka|soliq|kommunal|" +
            "xarid|sotib\\s*olish|narx|narxi|summa|taxminan|evaziga|" +
            "so['‘`]?m|som|сом|ming\\b|минг\\b|min\\b|мин\\b|k\\b|к\\b|mln\\b|млн\\b|million\\b|миллион\\b|dollar|\\$|usd)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Recurrence patterns
    private static final Pattern REC_MONTHLY_DAY_PATTERN = Pattern.compile(
            "har\\s*oyning\\s*(\\d{1,2})(?:-)?(?:kuni|kunida|sida|chi|chisi)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REC_EVERY_N_DAYS_PATTERN = Pattern.compile(
            "har\\s*(\\d+)\\s*kunda",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isTaskMessage(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = normalizeCyrillicToLatin(text.toLowerCase().trim());

        // If it explicitly matches past-tense expense AND has no task trigger: it's an expense
        boolean hasPastExpense = PAST_EXPENSE_PATTERN.matcher(lower).find();
        boolean hasTaskTrigger = TASK_TRIGGER_PATTERN.matcher(lower).find();

        if (hasPastExpense && !hasTaskTrigger) {
            return false;
        }

        // If it starts with /vazifa, /todo, /task, /eslat
        if (lower.startsWith("/vazifa") || lower.startsWith("/todo") || lower.startsWith("/task") || lower.startsWith("/eslat")) {
            return true;
        }

        if (hasTaskTrigger) {
            return true;
        }

        // Time or Date combined with an event/activity (e.g. "soat 12-da miting bor")
        boolean hasTimeOrDate = TIME_OR_DATE_PATTERN.matcher(lower).find();
        boolean hasEventOrAction = EVENT_OR_ACTION_PATTERN.matcher(lower).find();

        return hasTimeOrDate && hasEventOrAction;
    }

    public List<ParsedTodo> parseMultiOrSingle(String text, ZoneId zoneId) {
        if (text == null || text.isBlank()) return List.of();

        String cleaned = text.trim();
        // Remove command prefix if any
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.replaceFirst("^/[a-zA-Z0-9_]+\\s*", "");
        }

        // Check if message contains multiple lines with list numbering: "1. ... \n 2. ..."
        String[] lines = cleaned.split("\\r?\\n");
        List<String> taskLines = new ArrayList<>();
        if (lines.length > 1) {
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.matches("^[-*•\\d]+[.)]?\\s+.*")) {
                    taskLines.add(trimmedLine.replaceFirst("^[-*•\\d]+[.)]?\\s+", ""));
                }
            }
        }

        if (taskLines.size() > 1) {
            List<ParsedTodo> result = new ArrayList<>();
            for (String taskLine : taskLines) {
                parseSingle(taskLine, zoneId).ifPresent(result::add);
            }
            if (!result.isEmpty()) return result;
        }

        return parseSingle(cleaned, zoneId).map(List::of).orElse(List.of());
    }

    public Optional<ParsedTodo> parseSingle(String text, ZoneId zoneId) {
        if (text == null || text.isBlank()) return Optional.empty();

        String raw = text.trim();
        if (raw.startsWith("/")) {
            raw = raw.replaceFirst("^/[a-zA-Z0-9_]+\\s*", "");
        }
        String lower = normalizeCyrillicToLatin(raw.toLowerCase());

        // Check if it's strictly a past expense
        boolean hasPast = PAST_EXPENSE_PATTERN.matcher(lower).find();
        boolean hasTask = TASK_TRIGGER_PATTERN.matcher(lower).find();
        boolean isExpense = hasPast && !hasTask;

        // 1. Amount extraction - ONLY if financial intent or explicit currency/multiplier is present
        BigDecimal amount = null;
        boolean hasFinancialIntent = FINANCIAL_OR_AMOUNT_PATTERN.matcher(lower).find();
        if (hasFinancialIntent) {
            // Strip time and date expressions so that "soat 9 da", "soati 7da", "18:00", "12-da" are not parsed as amount
            String textForAmount = lower;
            textForAmount = textForAmount.replaceAll("(?i)\\b(?:soat(?:i|iga|ida)?\\s*)?\\d{1,2}[:.]\\d{2}(?:[-–\\s]*(?:da|ga|dagi|gacha))?\\b", " ");
            textForAmount = textForAmount.replaceAll("(?i)\\b(?:soat(?:i|iga|ida)?\\s*)?\\d{1,2}(?:[-–\\s]*(?:da|ga|dagi|gacha))\\b", " ");
            textForAmount = textForAmount.replaceAll("(?i)\\bsoat(?:i|iga|ida)?\\s*\\d{1,2}\\b", " ");
            textForAmount = textForAmount.replaceAll("(?i)\\bhar\\s*oyning\\s*\\d{1,2}(?:-)?(?:kuni|kunida|sida|chi|chisi)?\\b", " ");
            textForAmount = textForAmount.replaceAll("(?i)\\b\\d{1,2}(?:-)?(?:yanvar|fevral|mart|aprel|may|iyun|iyul|avgust|sentabr|oktabr|noyabr|dekabr)\\b", " ");

            amount = amountParser.parse(textForAmount).orElse(null);

            // Double check: if amount is suspiciously small (< 1000) and no explicit currency word was in text, drop it
            if (amount != null && amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
                boolean hasExplicitCurrency = textForAmount.matches(".*\\b(?:so['‘`]?m|som|сом|dollar|\\$|usd|rubl|yevro|evro)\\b.*");
                if (!hasExplicitCurrency) {
                    amount = null;
                }
            }
        }

        // 2. Date extraction
        LocalDate today = LocalDate.now(zoneId);
        LocalDate dueDate = dateParser.parseDate(lower, zoneId).orElse(null);

        // 3. Time extraction
        LocalTime dueTime = null;
        boolean hasSpecificTime = false;
        boolean isAmbiguous = false;

        Matcher explicitMatcher = TIME_EXPLICIT_PATTERN.matcher(lower);
        if (explicitMatcher.find()) {
            int h = Integer.parseInt(explicitMatcher.group(1));
            int m = Integer.parseInt(explicitMatcher.group(2));
            if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                dueTime = LocalTime.of(h, m);
                hasSpecificTime = true;
            }
        } else {
            Matcher hourMatcher = HOUR_ONLY_PATTERN.matcher(lower);
            if (hourMatcher.find()) {
                int h = Integer.parseInt(hourMatcher.group(1));
                String matchStr = hourMatcher.group(0).toLowerCase();
                if (matchStr.contains("kechki") || matchStr.contains("kechqurun") || matchStr.contains("oqshom")) {
                    if (h < 12) h += 12;
                } else if (matchStr.contains("ertalab") || matchStr.contains("saharda")) {
                    // Morning, keep 0-11
                } else if (matchStr.contains("tushda") || matchStr.contains("kunduzi")) {
                    if (h <= 5) h += 12;
                } else if (h >= 1 && h <= 6) {
                    isAmbiguous = true;
                    h += 12; // 1..6 without descriptor default to afternoon 13..18
                } else if (h == 7) {
                    LocalTime now = LocalTime.now(zoneId);
                    if (dueDate == null || dueDate.equals(today)) {
                        if (now.getHour() >= 12) {
                            // In afternoon/evening, "7 da" (e.g. "o'yin bor", "uchrashuv") means 19:00 (7 PM)
                            h = 19;
                        }
                    }
                }
                if (h >= 0 && h <= 23) {
                    dueTime = LocalTime.of(h, 0);
                    hasSpecificTime = true;
                }
            }
        }

        // If time was specified for "bugun" and the time has already passed today, don't silently shift date unless intentional
        if (dueDate == null && hasSpecificTime) {
            dueDate = today;
        }

        // 4. Recurrence extraction
        TodoRecurrenceType recurrenceType = TodoRecurrenceType.NONE;
        String daysOfWeek = null;
        Integer intervalDays = null;
        Integer dayOfMonth = null;

        Matcher monthlyMatcher = REC_MONTHLY_DAY_PATTERN.matcher(lower);
        if (monthlyMatcher.find()) {
            recurrenceType = TodoRecurrenceType.MONTHLY;
            dayOfMonth = Math.min(31, Math.max(1, Integer.parseInt(monthlyMatcher.group(1))));
            if (dueDate == null) {
                int targetDay = Math.min(dayOfMonth, today.lengthOfMonth());
                LocalDate candidate = today.withDayOfMonth(targetDay);
                dueDate = candidate.isBefore(today) ? today.plusMonths(1).withDayOfMonth(Math.min(dayOfMonth, today.plusMonths(1).lengthOfMonth())) : candidate;
            }
        } else if (lower.contains("har oy") || lower.contains("oyma-oy") || lower.contains("каждый месяц")) {
            recurrenceType = TodoRecurrenceType.MONTHLY;
            dayOfMonth = today.getDayOfMonth();
        } else if (lower.contains("har kuni") || lower.contains("har kun") || lower.contains("kunlik") || lower.contains("каждый день")) {
            recurrenceType = TodoRecurrenceType.DAILY;
        } else if (lower.contains("har hafta") || lower.contains("haftalik") || lower.contains("каждую неделю")) {
            recurrenceType = TodoRecurrenceType.WEEKLY;
            daysOfWeek = today.getDayOfWeek().name();
        } else if (REC_EVERY_N_DAYS_PATTERN.matcher(lower).find()) {
            Matcher m = REC_EVERY_N_DAYS_PATTERN.matcher(lower);
            if (m.find()) {
                recurrenceType = TodoRecurrenceType.EVERY_N_DAYS;
                intervalDays = Integer.parseInt(m.group(1));
            }
        }

        // 5. Priority extraction
        TodoPriority priority = TodoPriority.MEDIUM;
        if (lower.contains("muhim") || lower.contains("shoshilinch") || lower.contains("urgent") || lower.contains("срочно") || lower.contains("yuqori")) {
            priority = TodoPriority.HIGH;
        } else if (lower.contains("past") || lower.contains("shoshilmas") || lower.contains("keyinroq") || lower.contains("bosh vaqtda")) {
            priority = TodoPriority.LOW;
        }

        // 6. Category extraction
        String categoryName = categoryMatcher.matchCategory(raw, com.hisobchi.bot.transaction.entity.TransactionType.EXPENSE).orElse(null);

        // 7. Clean Title Construction
        String title = cleanTaskTitle(raw);
        if (title.isBlank()) {
            title = raw;
        }

        return Optional.of(ParsedTodo.builder()
                .title(title)
                .description(null)
                .dueDate(dueDate)
                .dueTime(dueTime)
                .hasSpecificTime(hasSpecificTime)
                .reminderMinutesBefore(hasSpecificTime ? 0 : null)
                .recurrenceType(recurrenceType)
                .recurrenceDaysOfWeek(daysOfWeek)
                .recurrenceIntervalDays(intervalDays)
                .recurrenceDayOfMonth(dayOfMonth)
                .priority(priority)
                .categoryName(categoryName)
                .plannedAmount(amount)
                .currency("UZS")
                .isExpenseIntent(isExpense)
                .isAmbiguousTime(isAmbiguous)
                .confidence(0.9)
                .rawText(raw)
                .build());
    }

    private String cleanTaskTitle(String text) {
        String cleaned = text.trim();
        // Remove common prompt phrases at beginning:
        // "Ertaga 18:00 da ... ni eslat", "eslat", "eslatgin"
        cleaned = cleaned.replaceAll("(?i)\\b(?:eslat|eslatgin|eslatvor|napomni|напомни|iltimos|shu)\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(?:to['‘`]?lashni|qilishni|bajarishni|olishni|borishni)\\s*eslat\\b", "");
        // Clean multiple spaces
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 1) {
            cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
        }
        return cleaned;
    }

    private String normalizeCyrillicToLatin(String input) {
        if (input == null) return "";
        return input
                .replace("эртага", "ertaga")
                .replace("индинга", "indinga")
                .replace("бугун", "bugun")
                .replace("жума", "juma")
                .replace("душанба", "dushanba")
                .replace("сешанба", "seshanba")
                .replace("чоршанба", "chorshanba")
                .replace("пайшанба", "payshanba")
                .replace("шанба", "shanba")
                .replace("якшанба", "yakshanba")
                .replace("эслат", "eslat")
                .replace("соат", "soat")
                .replace("минг", "ming")
                .replace("млн", "mln")
                .replace("тўлаш", "to'lash")
                .replace("тўладим", "to'ladim")
                .replace("завтра", "ertaga")
                .replace("послезавтра", "indinga")
                .replace("сегодня", "bugun")
                .replace("напомни", "eslat")
                .replace("оплатить", "to'lash")
                .replace("тысяч", "ming")
                .replace("миллион", "million");
    }
}
