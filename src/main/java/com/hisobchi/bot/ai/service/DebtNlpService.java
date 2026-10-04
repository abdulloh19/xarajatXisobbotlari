package com.hisobchi.bot.ai.service;

import com.hisobchi.bot.ai.dto.DebtIntent;
import com.hisobchi.bot.ai.dto.ParsedDebt;
import com.hisobchi.bot.common.util.UzbekDateParser;
import com.hisobchi.bot.debt.entity.DebtType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtNlpService {

    private final UzbekAmountParser amountParser;
    private final UzbekDateParser dateParser;

    // Pattern to capture person name with Uzbek case endings (-dan, -ga, -ka, -qa):
    // "Rustam akadan", "Akmal akadan", "Rustamga", "Rustam akaga", "Javlonga", "Boburdan"
    // Pattern to capture person name with Uzbek case endings (-dan, -ga, -ka, -qa):
    // "Rustam akadan", "Akmal akadan", "Rustamga", "Rustam akaga", "Javlonga", "Boburdan", "moydan", "zapravkadan", "dokondan"
    private static final Pattern PERSON_SUFFIX_PATTERN = Pattern.compile(
            "\\b([A-ZА-Яa-zа-я'‘`]+(?:\\s+(?:aka|opa|uka|singil|tog'a|toga|amaki|xola|pochcha))?)(?:dan|ga|ka|qa)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Pattern to capture entity immediately preceding "qarzman", "qarz oldim", etc.
    // "moy qarzman", "benzin uchun qarzman", "moydan qarzman"
    private static final Pattern PRE_DEBT_PATTERN = Pattern.compile(
            "\\b([A-ZА-Яa-zа-я'‘`]+(?:\\s+(?:aka|opa|uka|singil|tog'a|toga|amaki|xola|pochcha))?)\\s+(?:uchun\\s+)?(?:qarzman|qarzdorman|qarz\\s+oldim|qarz\\s+olganman|qarz\\s+oganman)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Pattern to capture leading subject person in return phrases: "Javlon 600 ming qarz qaytardi", "Javlon hamma qarzini qaytardi"
    private static final Pattern LEADING_PERSON_PATTERN = Pattern.compile(
            "^([A-ZА-Яa-zа-я'‘`]+(?:\\s+(?:aka|opa|uka|singil|tog'a|toga|amaki|xola|pochcha))?)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    public Optional<ParsedDebt> parse(String text, ZoneId zoneId) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String lower = text.toLowerCase().trim();

        // Check if message is related to debt
        boolean hasDebtWord = lower.contains("qarz")
                || lower.contains("qarzman")
                || lower.contains("qarzdor")
                || lower.contains("qarzdorman")
                || lower.contains("qarzga")
                || lower.contains("qarzim")
                || lower.contains("qarzini")
                || lower.contains("nasiya")
                || lower.contains("nasiyaga");
        boolean hasReturnWord = lower.contains("qaytardi") || lower.contains("qaytdi") || lower.contains("qaytardim");
        boolean hasBorrowVerb = lower.contains("oldim") || lower.contains("olganman") || lower.contains("oganman") || lower.contains("berishim kerak") || lower.contains("beraman");
        boolean hasLentVerb = lower.contains("berdim") || lower.contains("berganman") || lower.contains("beradi") || lower.contains("berishi kerak");

        if (!hasDebtWord && !hasReturnWord && !hasBorrowVerb && !hasLentVerb) {
            return Optional.empty();
        }

        // Determine Intent
        DebtIntent intent = detectIntent(lower);
        if (intent == null) {
            return Optional.empty();
        }

        DebtType type = (intent == DebtIntent.BORROW || intent == DebtIntent.REPAY_PARTIAL || intent == DebtIntent.REPAY_FULL)
                ? DebtType.BORROWED
                : DebtType.LENT;

        // Amount extraction
        Optional<BigDecimal> amountOpt = amountParser.parse(text);
        BigDecimal amount = amountOpt.orElse(null);

        // If intent requires amount and amount is missing, but not a FULL repayment/return
        if (amount == null && intent != DebtIntent.REPAY_FULL && intent != DebtIntent.RETURN_FULL) {
            // Could still be a debt creation where amount is missing, but if amount is completely absent and no full keyword, return empty
            return Optional.empty();
        }

        // Person extraction
        String personName = extractPersonName(text, lower, intent);
        boolean missingPerson = personName == null || personName.isBlank() || "Noma'lum".equalsIgnoreCase(personName);

        // Due date extraction
        Optional<LocalDate> dueDateOpt = dateParser.parseDate(text, zoneId);
        LocalDate dueDate = dueDateOpt.orElse(null);
        boolean missingDueDate = (dueDate == null && (intent == DebtIntent.BORROW || intent == DebtIntent.LEND));

        // Payment method extraction
        String paymentMethod = null;
        if (lower.contains("karta") || lower.contains("kartadan") || lower.contains("plastik") || lower.contains("hisob raqam")) {
            paymentMethod = "Karta";
        } else if (lower.contains("naqd") || lower.contains("qo'lga") || lower.contains("qolga")) {
            paymentMethod = "Naqd";
        }
        boolean missingPaymentMethod = (paymentMethod == null);

        double confidence = 0.80;
        if (!missingPerson) confidence += 0.10;
        if (amount != null || intent == DebtIntent.REPAY_FULL || intent == DebtIntent.RETURN_FULL) confidence += 0.05;

        log.info("Parsed debt NLP: intent={}, type={}, amount={}, person={}, dueDate={}, missingPerson={}, missingDueDate={}, missingMethod={}",
                intent, type, amount, personName, dueDate, missingPerson, missingDueDate, missingPaymentMethod);

        return Optional.of(new ParsedDebt(
                intent,
                type,
                amount,
                personName != null ? personName : "Noma'lum",
                dueDate,
                text.trim(),
                confidence,
                text.trim(),
                paymentMethod != null ? paymentMethod : "Naqd",
                missingPerson,
                missingDueDate,
                missingPaymentMethod
        ));
    }

    private DebtIntent detectIntent(String lower) {
        // 1. Full debt repayment: "Rustam akaga qarzimni hammasini berdim", "qarzimni hammasini to'ladim"
        if ((lower.contains("hammasini berdim") || lower.contains("hammasini to'ladim") || lower.contains("hammasini toladim")
                || lower.contains("qarzimni yopdim") || lower.contains("to'liq to'ladim") || lower.contains("toliq toladim")
                || lower.contains("barchasini berdim")) && (lower.contains("qarz") || lower.contains("berdim") || lower.contains("to'ladim") || lower.contains("toladim"))) {
            return DebtIntent.REPAY_FULL;
        }

        // 2. Full debt return by other person: "Javlon hamma qarzini qaytardi", "Javlon qarzini hammasini qaytardi"
        if ((lower.contains("hammasini qaytardi") || lower.contains("hamma qarzini qaytardi") || lower.contains("to'liq qaytardi")
                || lower.contains("toliq qaytardi") || lower.contains("barchasini qaytardi")) && (lower.contains("qarz") || lower.contains("qaytardi"))) {
            return DebtIntent.RETURN_FULL;
        }

        // 3. Partial repayment of user's debt: "Rustam akaga qarzimdan 500 ming berdim", "qarzimdan 200 ming to'ladim"
        if (lower.contains("qarzimdan") || lower.contains("qarzimga") || lower.contains("qarzga berdim")
                || (lower.contains("qarz") && (lower.contains("to'ladim") || lower.contains("toladim")))
                || (lower.contains("qarzim") && lower.contains("berdim"))) {
            return DebtIntent.REPAY_PARTIAL;
        }

        // 4. Return from other person: "Javlon 600 ming qarz qaytardi", "Javlon 600 ming qarzini qaytardi", "qarz qaytardi", "qarz qaytdi"
        if (lower.contains("qarz qaytardi") || lower.contains("qarzini qaytardi") || lower.contains("qarz qaytdi")
                || (lower.contains("qaytardi") && lower.contains("qarz")) || (lower.contains("qaytarib berdi") && lower.contains("qarz"))) {
            return DebtIntent.RETURN_PARTIAL;
        }

        // 5. Creating BORROWED debt (User owes money / borrowed money):
        // "250min qarzman", "moydan 250 ming qarzman", "qarz oldim", "qarz olganman", "qarz bo'ldim"
        if (lower.contains("qarzman")
                || lower.contains("qarzdorman")
                || lower.contains("qarz oldim")
                || lower.contains("qarz olganman")
                || lower.contains("qarz oganman")
                || lower.contains("qarz ogamman")
                || lower.contains("qarz boldim")
                || lower.contains("qarz bo'ldim")
                || lower.contains("qarz bo‘ldim")
                || lower.contains("qarzga oldim")
                || lower.contains("qarzga oganman")
                || lower.contains("nasiyaga oldim")
                || lower.contains("nasiya")
                || (lower.contains("qarz") && (lower.contains("oldim") || lower.contains("olganman") || lower.contains("oganman") || lower.contains("olindi") || lower.contains("olgan")))
                || (lower.contains("oldim") && !lower.contains("berdim") && !lower.contains("qarz berdim"))) {
            return DebtIntent.BORROW;
        }

        // 6. Creating LENT debt (User lent money to someone else):
        if (lower.contains("qarz berdim") || lower.contains("qarzga berdim") || (lower.contains("berdim") && !lower.contains("qarzimdan") && !lower.contains("hammasini"))) {
            return DebtIntent.LEND;
        }

        // Fallbacks
        if (lower.contains("berishim kerak") || lower.contains("qaytaraman") || lower.contains("qarzim bor")) {
            return DebtIntent.BORROW;
        }

        if (lower.contains("qaytaradi") || lower.contains("berishi kerak")) {
            return DebtIntent.LEND;
        }

        return null;
    }

    private String extractPersonName(String text, String lower, DebtIntent intent) {
        // 1. Suffix match: "Rustam akadan", "moydan", "zapravkadan", "do'kondan"
        Matcher suffixMatcher = PERSON_SUFFIX_PATTERN.matcher(text);
        while (suffixMatcher.find()) {
            String candidate = suffixMatcher.group(1).trim();
            String candLower = candidate.toLowerCase();
            if (!isFilteredWord(candLower)) {
                return capitalizeWords(candidate);
            }
        }

        // 2. Pre-debt match: "moy qarzman", "benzin uchun qarzman"
        Matcher preMatcher = PRE_DEBT_PATTERN.matcher(text);
        if (preMatcher.find()) {
            String candidate = preMatcher.group(1).trim();
            String candLower = candidate.toLowerCase();
            if (!isFilteredWord(candLower)) {
                return capitalizeWords(candidate);
            }
        }

        // 3. Leading person subject: "Javlon 600 ming qarz qaytardi", "Javlon hamma qarzini qaytardi"
        Matcher leadMatcher = LEADING_PERSON_PATTERN.matcher(text);
        if (leadMatcher.find()) {
            String candidate = leadMatcher.group(1).trim();
            String candLower = candidate.toLowerCase();
            if (!isFilteredWord(candLower)) {
                return capitalizeWords(candidate);
            }
        }

        return null;
    }

    private static final Set<String> FILTERED_EXACT_WORDS = Set.of(
            "bugun", "kecha", "ertaga", "indin", "ertalab", "kechqurun", "kunduzi",
            "oy", "hafta", "yil", "kun",
            "karta", "kartadan", "kartaga", "plastik", "plastikdan", "naqd", "naqdga",
            "hisob", "hisobdan", "hisobimdan",
            "men", "menga", "meni", "mening", "sen", "sendan", "senga",
            "siz", "sizdan", "sizga", "u", "unga", "undan",
            "biz", "bizdan", "ular", "ulardan",
            "ozim", "o'zim", "o‘zim",
            "oldim", "olganman", "oganman", "berdim", "berganman",
            "qaytardi", "qaytardim", "toladim", "to'ladim", "to‘ladim",
            "boldim", "bo'ldim", "bo‘ldim",
            "ming", "min", "mln", "million", "som", "so'm", "so‘m", "sum",
            "pul", "pulni", "summa", "xarajat", "daromad", "foyda"
    );

    private boolean isFilteredWord(String word) {
        if (word == null || word.isBlank()) return true;
        String[] parts = word.trim().toLowerCase().split("\\s+");
        boolean allFiltered = true;
        for (String p : parts) {
            boolean isPFiltered = FILTERED_EXACT_WORDS.contains(p)
                    || p.startsWith("qarz")
                    || p.startsWith("nasiya")
                    || p.startsWith("hamma")
                    || p.startsWith("barcha");
            if (!isPFiltered) {
                allFiltered = false;
                break;
            }
        }
        return allFiltered;
    }

    public static String cleanEntityName(String input) {
        if (input == null || input.isBlank()) return "Noma'lum";
        String t = input.trim();
        // Remove trailing case suffixes -dan, -ga, -ka, -qa, -uchun
        t = t.replaceAll("(?i)(?:dan|ga|ka|qa|uchun)$", "").trim();
        if (t.isBlank()) return input.trim();
        return Character.toUpperCase(t.charAt(0)) + (t.length() > 1 ? t.substring(1) : "");
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isBlank()) return input;
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
