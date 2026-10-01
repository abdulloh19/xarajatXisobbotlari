package com.hisobchi.bot.ai.service;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtNlpService {

    private final UzbekAmountParser amountParser;
    private final UzbekDateParser dateParser;

    // Pattern to capture person name with Uzbek case endings (-dan, -ga, -ka, -qa):
    // "Rustam akadan", "Akmal akadan", "Javlonga", "Boburdan", "Sherzodga"
    private static final Pattern PERSON_PATTERN = Pattern.compile(
            "\\b([A-ZА-Яa-zа-я'‘`]+(?:\\s+(?:aka|opa|uka|singil|tog'a|toga|amaki|xola|pochcha))?)(?:dan|ga|ka|qa)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    public Optional<ParsedDebt> parse(String text, ZoneId zoneId) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String lower = text.toLowerCase().trim();

        // 1. Debt Intent check
        boolean hasDebtWord = lower.contains("qarz") || lower.contains("qarzga");
        boolean hasBorrowedVerb = lower.contains("oldim") || lower.contains("berishim kerak") || lower.contains("beraman") || lower.contains("qaytaraman");
        boolean hasLentVerb = lower.contains("berdim") || lower.contains("qaytaradi") || lower.contains("beradi") || lower.contains("berishi kerak");

        // Must have debt indicator
        if (!hasDebtWord && !(hasBorrowedVerb && lower.matches(".*(?:dan|ga).*")) && !(hasLentVerb && lower.matches(".*(?:ga|qa|ka).*"))) {
            return Optional.empty();
        }

        // 2. Determine DebtType: BORROWED vs LENT
        DebtType type;
        if (lower.contains("qarz oldim") || (lower.contains("oldim") && !lower.contains("qarz berdim"))) {
            type = DebtType.BORROWED;
        } else if (lower.contains("qarz berdim") || lower.contains("berdim") || lower.contains("qaytaradi")) {
            type = DebtType.LENT;
        } else if (lower.contains("berishim kerak") || lower.contains("qaytaraman")) {
            type = DebtType.BORROWED;
        } else {
            type = DebtType.BORROWED;
        }

        // 3. Amount extraction
        Optional<BigDecimal> amountOpt = amountParser.parse(text);
        if (amountOpt.isEmpty()) {
            log.debug("Debt intent detected but amount missing in: '{}'", text);
            return Optional.empty();
        }
        BigDecimal amount = amountOpt.get();

        // 4. Person name extraction
        String personName = "Noma'lum";
        Matcher personMatcher = PERSON_PATTERN.matcher(text);
        while (personMatcher.find()) {
            String candidate = personMatcher.group(1).trim();
            // Filter out common non-name words that end in -dan/-ga
            String candLower = candidate.toLowerCase();
            if (!candLower.matches(".*(?:bugun|kecha|ertalab|kechqurun|oy|hafta|karta|naqd|bank|bozor|dokon|magazin|zapravka|taksi|tushlik|obed).*")) {
                personName = capitalizeWords(candidate);
                break;
            }
        }

        // 5. Due date extraction
        Optional<LocalDate> dueDateOpt = dateParser.parseDate(text, zoneId);
        LocalDate dueDate = dueDateOpt.orElse(null);

        // 6. Payment method extraction
        String paymentMethod = "Naqd";
        if (lower.contains("karta") || lower.contains("kartadan") || lower.contains("plastik") || lower.contains("hisob raqam")) {
            paymentMethod = "Karta";
        }

        double confidence = (dueDate != null && !"Noma'lum".equals(personName)) ? 0.95 : 0.80;

        log.info("Parsed debt from text: type={}, amount={}, person={}, dueDate={}, paymentMethod={}",
                type, amount, personName, dueDate, paymentMethod);

        return Optional.of(new ParsedDebt(
                type,
                amount,
                personName,
                dueDate,
                text.trim(),
                confidence,
                text.trim(),
                paymentMethod
        ));
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
