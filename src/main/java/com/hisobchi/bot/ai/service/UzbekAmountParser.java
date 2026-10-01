package com.hisobchi.bot.ai.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UzbekAmountParser {

    private static final Map<String, Long> NUMBER_WORDS = new HashMap<>();

    static {
        NUMBER_WORDS.put("nol", 0L);
        NUMBER_WORDS.put("bir", 1L);
        NUMBER_WORDS.put("ikki", 2L);
        NUMBER_WORDS.put("uch", 3L);
        NUMBER_WORDS.put("to'rt", 4L);
        NUMBER_WORDS.put("tort", 4L);
        NUMBER_WORDS.put("besh", 5L);
        NUMBER_WORDS.put("olti", 6L);
        NUMBER_WORDS.put("yetti", 7L);
        NUMBER_WORDS.put("etti", 7L);
        NUMBER_WORDS.put("sakkiz", 8L);
        NUMBER_WORDS.put("to'qqiz", 9L);
        NUMBER_WORDS.put("toqqiz", 9L);

        NUMBER_WORDS.put("o'n", 10L);
        NUMBER_WORDS.put("on", 10L);
        NUMBER_WORDS.put("yigirma", 20L);
        NUMBER_WORDS.put("o'ttiz", 30L);
        NUMBER_WORDS.put("ottiz", 30L);
        NUMBER_WORDS.put("qirq", 40L);
        NUMBER_WORDS.put("elli", 50L);
        NUMBER_WORDS.put("eliy", 50L);
        NUMBER_WORDS.put("ellik", 50L);
        NUMBER_WORDS.put("oltmish", 60L);
        NUMBER_WORDS.put("yetmish", 70L);
        NUMBER_WORDS.put("etmish", 70L);
        NUMBER_WORDS.put("sakson", 80L);
        NUMBER_WORDS.put("to'qson", 90L);
        NUMBER_WORDS.put("toqson", 90L);

        NUMBER_WORDS.put("yuz", 100L);
        NUMBER_WORDS.put("min", 1_000L);
        NUMBER_WORDS.put("мин", 1_000L);
        NUMBER_WORDS.put("ming", 1_000L);
        NUMBER_WORDS.put("минг", 1_000L);
        NUMBER_WORDS.put("million", 1_000_000L);
        NUMBER_WORDS.put("миллион", 1_000_000L);
        NUMBER_WORDS.put("mln", 1_000_000L);
        NUMBER_WORDS.put("млн", 1_000_000L);
        NUMBER_WORDS.put("milliard", 1_000_000_000L);
    }

    // Pattern for "2 yarim million", "ikki yarim million", "yarim million"
    private static final Pattern HALF_MILLION_PATTERN = Pattern.compile(
            "(?:(\\d+|bir|ikki|uch|to'rt|tort|besh|olti|yetti|etti|sakkiz|to'qqiz|toqqiz)\\s+)?yarim\\s+(?:million|mln|миллион|млн)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Pattern for "1 million 200 ming", "1 mln 250 ming", "1 mln 200"
    private static final Pattern MILLION_AND_THOUSAND_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:million|mln|миллион|млн)\\s*(?:va\\s*)?(\\d+)(?:\\s*ming|\\s*минг)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Pattern for decimal million: "1.2 million", "1,5 mln"
    private static final Pattern DECIMAL_MILLION_PATTERN = Pattern.compile(
            "(\\d+[.,]\\d+)\\s*(?:million|mln|миллион|млн)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Pattern for single million: "2 million", "5 mln"
    private static final Pattern SINGLE_MILLION_PATTERN = Pattern.compile(
            "(\\d+)\\s*(?:million|mln|миллион|млн)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Pattern for thousand: "15 ming", "15 min", "15k", "15 минг", "1.5 ming"
    private static final Pattern THOUSAND_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:ming|минг|min|мин|k|к)(?:\\s*(?:so['‘`]?m|som|сом))?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Pattern for standalone clean digits: "15 000", "15000", "15 000 so'm"
    private static final Pattern DIGITS_PATTERN = Pattern.compile(
            "\\b(\\d{1,3}(?:[\\s_]\\d{3})+|\\d{3,12})(?:\\s*(?:so['‘`]?m|som|сом))?\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    public Optional<BigDecimal> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(text);

        // 1. Check "X yarim million"
        Matcher halfMatcher = HALF_MILLION_PATTERN.matcher(normalized);
        if (halfMatcher.find()) {
            String lead = halfMatcher.group(1);
            BigDecimal base = BigDecimal.ZERO;
            if (lead != null && !lead.isBlank()) {
                base = parseLeadNumber(lead);
            }
            BigDecimal result = base.multiply(BigDecimal.valueOf(1_000_000))
                    .add(BigDecimal.valueOf(500_000));
            return Optional.of(result);
        }

        // 2. Check "1 million 200 ming" or "1 mln 250"
        Matcher milThousMatcher = MILLION_AND_THOUSAND_PATTERN.matcher(normalized);
        if (milThousMatcher.find()) {
            String milStr = milThousMatcher.group(1).replace(',', '.');
            String thousStr = milThousMatcher.group(2);
            BigDecimal mil = new BigDecimal(milStr).multiply(BigDecimal.valueOf(1_000_000));
            BigDecimal thous = new BigDecimal(thousStr);
            if (thous.compareTo(BigDecimal.valueOf(1000)) < 0) {
                thous = thous.multiply(BigDecimal.valueOf(1000));
            }
            return Optional.of(mil.add(thous).setScale(0, RoundingMode.HALF_UP));
        }

        // 3. Check decimal million: "1.2 million", "1.5 mln"
        Matcher decMilMatcher = DECIMAL_MILLION_PATTERN.matcher(normalized);
        if (decMilMatcher.find()) {
            String valStr = decMilMatcher.group(1).replace(',', '.');
            BigDecimal val = new BigDecimal(valStr).multiply(BigDecimal.valueOf(1_000_000));
            return Optional.of(val.setScale(0, RoundingMode.HALF_UP));
        }

        // 4. Check single million: "2 million", "5 mln"
        Matcher singleMilMatcher = SINGLE_MILLION_PATTERN.matcher(normalized);
        if (singleMilMatcher.find()) {
            BigDecimal val = new BigDecimal(singleMilMatcher.group(1)).multiply(BigDecimal.valueOf(1_000_000));
            return Optional.of(val);
        }

        // 5. Check thousand: "15 ming", "15k", "300 ming"
        Matcher thousMatcher = THOUSAND_PATTERN.matcher(normalized);
        if (thousMatcher.find()) {
            String valStr = thousMatcher.group(1).replace(',', '.');
            BigDecimal val = new BigDecimal(valStr).multiply(BigDecimal.valueOf(1000));
            return Optional.of(val.setScale(0, RoundingMode.HALF_UP));
        }

        // 6. Check formatted or plain digits: "15 000", "15000", "1250000"
        Matcher digitsMatcher = DIGITS_PATTERN.matcher(normalized);
        if (digitsMatcher.find()) {
            String raw = digitsMatcher.group(1).replaceAll("[\\s_]", "");
            try {
                BigDecimal val = new BigDecimal(raw);
                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    return Optional.of(val);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 7. Check full words: "bir million ikki yuz ming", "o'n besh ming"
        Optional<BigDecimal> wordsResult = parseWords(normalized);
        if (wordsResult.isPresent()) {
            return wordsResult;
        }

        return Optional.empty();
    }

    private BigDecimal parseLeadNumber(String wordOrDigit) {
        if (wordOrDigit.matches("\\d+")) {
            return new BigDecimal(wordOrDigit);
        }
        Long val = NUMBER_WORDS.get(wordOrDigit.toLowerCase());
        return val != null ? BigDecimal.valueOf(val) : BigDecimal.ONE;
    }

    private Optional<BigDecimal> parseWords(String text) {
        String[] tokens = text.toLowerCase().split("\\s+");
        long current = 0;
        long total = 0;
        boolean foundNumber = false;

        for (String token : tokens) {
            // Clean token of punctuation
            String clean = token.replaceAll("[^a-z'‘`а-я0-9]", "");
            if (clean.isBlank()) continue;

            if (NUMBER_WORDS.containsKey(clean)) {
                foundNumber = true;
                long val = NUMBER_WORDS.get(clean);
                if (val == 1_000_000L || val == 1_000_000_000L) {
                    current = (current == 0) ? 1 : current;
                    total += current * val;
                    current = 0;
                } else if (val == 1_000L) {
                    current = (current == 0) ? 1 : current;
                    total += current * 1_000L;
                    current = 0;
                } else if (val == 100L) {
                    current = (current == 0) ? 1 : current;
                    current *= 100L;
                } else {
                    current += val;
                }
            } else if (clean.matches("\\d+")) {
                foundNumber = true;
                current += Long.parseLong(clean);
            }
        }

        total += current;
        if (foundNumber && total > 0) {
            return Optional.of(BigDecimal.valueOf(total));
        }
        return Optional.empty();
    }

    public String normalize(String input) {
        if (input == null) return "";
        String s = input
                .replace("’", "'")
                .replace("‘", "'")
                .replace("`", "'")
                .trim();
        // Replace acoustic artifact "ilmiy <expense>" with "50 ming <expense>"
        s = s.replaceAll("(?i)\\bilmiy\\s+(zapra[vf]ka|benzin|gaz|metan|propan|yoqilg['‘`]?i|taksi|obed|tushlik|ovqat|bozor|magazin|dori|paynet)\\b", "50 ming $1");
        s = s.replaceAll("(?i)\\bilmiy\\s*(?:ming|min)\\b", "50 ming");
        // Dialectal "elli min" / "ellik min" -> "50 ming"
        s = s.replaceAll("(?i)\\belli(?:k)?\\s*min\\b", "50 ming");
        // Digits followed by "min" -> "ming"
        s = s.replaceAll("(?i)\\b(\\d+)\\s*min(?:i|ga|dan)?\\b", "$1 ming");
        return s;
    }
}
