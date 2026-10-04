package com.hisobchi.bot.ai.service;

import com.hisobchi.bot.ai.dto.ParsedTransaction;
import com.hisobchi.bot.transaction.entity.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionNlpService {

    private final UzbekAmountParser amountParser;
    private final CategoryMatcher categoryMatcher;
    private final OpenAiNlpService openAiNlpService;

    public Optional<ParsedTransaction> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String cleaned = text.trim();
        log.debug("Parsing message text: '{}'", cleaned);

        // Guard against period/menu navigation strings like "Oxirgi 7 kun", "7 kun", "Hisobotlar", etc.
        String lower = cleaned.toLowerCase();
        if (lower.contains("oxirgi") || lower.matches(".*\\b\\d+\\s*(?:kun|hafta|oy|yil)\\b.*") || lower.contains("hisobot")) {
            if (!lower.contains("so'm") && !lower.contains("som") && !lower.contains("ming") && !lower.contains("mln")
                    && !lower.contains("xarajat") && !lower.contains("daromad") && !lower.contains("berdim") && !lower.contains("oldim")) {
                log.debug("Ignoring non-financial navigation/period text: '{}'", cleaned);
                return Optional.empty();
            }
        }

        // 1. Rule-based amount extraction
        Optional<BigDecimal> parsedAmount = amountParser.parse(cleaned);

        if (parsedAmount.isPresent()) {
            BigDecimal amount = parsedAmount.get();
            TransactionType type = categoryMatcher.detectIntent(cleaned);
            Optional<String> matchedCategory = categoryMatcher.matchCategory(cleaned, type);

            if (matchedCategory.isPresent()) {
                // High confidence rule-based match
                String cat = matchedCategory.get();
                String description = extractDescription(cleaned, cat);
                log.info("Rule-based NLP parse successful: type={}, amount={}, category={}, desc='{}'",
                        type, amount, cat, description);
                return Optional.of(new ParsedTransaction(type, amount, cat, description, 0.95, cleaned));
            } else {
                // Amount detected, but category is uncertain. Confidence is medium.
                log.info("Amount found ({}) but category is uncertain for: '{}'", amount, cleaned);
                // Attempt AI classification if enabled, to see if AI can detect category
                Optional<ParsedTransaction> aiResult = openAiNlpService.parseWithAi(cleaned);
                if (aiResult.isPresent() && aiResult.get().category() != null && !"Boshqa".equals(aiResult.get().category())) {
                    return aiResult;
                }
                // Return transaction with null category so user is prompted to pick a category!
                return Optional.of(new ParsedTransaction(type, amount, null, cleaned, 0.60, cleaned));
            }
        }

        // 2. Amount wasn't detected by rule parser, attempt AI fallback
        log.debug("Rule-based amount parser found nothing, attempting AI fallback for: '{}'", cleaned);
        return openAiNlpService.parseWithAi(cleaned);
    }

    private String extractDescription(String text, String category) {
        // Simple description extractor
        String desc = text.trim();
        // Capitalize first letter
        if (desc.length() > 0) {
            return Character.toUpperCase(desc.charAt(0)) + (desc.length() > 1 ? desc.substring(1) : "");
        }
        return desc;
    }
}
