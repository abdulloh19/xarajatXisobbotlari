package com.hisobchi.bot.ai.service;

import com.hisobchi.bot.transaction.entity.TransactionType;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CategoryMatcher {

    private static final Map<String, List<String>> EXPENSE_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, List<String>> INCOME_KEYWORDS = new LinkedHashMap<>();

    private static final Set<String> INCOME_INTENT_WORDS = Set.of(
            "ishladim", "daromad", "tushum", "tushdi", "toladi", "to'ladi",
            "berishdi", "gonorar", "avans", "foyda", "kirdi", "oldim"
    );

    private static final Set<String> EXPENSE_INTENT_WORDS = Set.of(
            "sarfladim", "ketdi", "berdim", "ishlatdim", "toladim", "to'ladim",
            "quydim", "xarajat", "oldim", "sarflandim"
    );

    static {
        // Oziq-ovqat
        EXPENSE_KEYWORDS.put("Oziq-ovqat", List.of(
                "obed", "абед", "tushlik", "non", "ovqat", "restoran", "kafe", "osh",
                "somsa", "lavash", "burger", "ichimlik", "choy", "kofe", "shashlik",
                "pitsa", "kechki ovqat", "go'sht", "gosht", "tandir", "donar",
                "bozor", "supermarket", "korzinka", "havas", "makro", "bozordan", "shirinlik"
        ));

        // Transport va Yoqilg‘i
        EXPENSE_KEYWORDS.put("Transport va Yoqilg‘i", List.of(
                "benzin", "benzinga", "gaz", "gazga", "metan", "metanga", "propan", "propanga",
                "zapravka", "zapravkaga", "zaprafka", "zaprafkaga",
                "yoqilg'i", "yoqilgi", "solyarka", "moy", "moyga", "propan quy",
                "taxi", "taksi", "yo'l", "yol", "yo'lga", "avtobus", "metro", "transport",
                "marshrutka", "proyezd", "yandex", "yandeks"
        ));

        // Ro‘zg‘or va Kommunal
        EXPENSE_KEYWORDS.put("Ro‘zg‘or va Kommunal", List.of(
                "svet", "elektr", "suv", "issiqlik", "gaz to'lovi", "musor", "kommunal",
                "arenda", "kvartira", "ijara", "mebel", "ta'mir", "remont", "uy",
                "internet", "wi-fi", "wifi", "balans", "tarif", "beeline", "ucell",
                "uztelecom", "mobiuz", "paynet", "telefon"
        ));

        // Oila va Salomatlik
        EXPENSE_KEYWORDS.put("Oila va Salomatlik", List.of(
                "bolalar", "maktab", "bog'cha", "pampers", "kiyim", "oila",
                "sovg'a", "sovga", "podarka", "to'yona", "toyona", "tug'ilgan kun",
                "apteka", "dori", "tabletka", "ukol", "shifoxona", "vrach", "poliklinika",
                "kino", "konsert", "aylanish", "dam olish", "park", "attraksion",
                "kurs", "repetitor", "kitob", "o'qish", "kontrakt", "talim",
                "bilet", "samolyot", "poyezd", "mehmonxona", "sayohat", "viza"
        ));

        // Biznes va Ish
        EXPENSE_KEYWORDS.put("Biznes va Ish", List.of(
                "material", "truba", "mis", "kabel", "izolyatsiya", "freon",
                "kronshteyn", "vozduxovod", "detal", "zapchast", "sement",
                "gips", "kraska", "samorez", "instrument", "bolt", "gayka",
                "usta", "ishchi", "montajchi", "xodim", "oylik", "ish haqi",
                "ish haqqi", "ishhaqi", "xizmat haqi",
                "kredit", "qarz", "nasiya", "to'lov", "foiz", "qarzga", "soliq"
        ));

        // Boshqa
        EXPENSE_KEYWORDS.put("Boshqa", List.of(
                "boshqa", "mayda"
        ));

        // INCOME KEYWORDS
        INCOME_KEYWORDS.put("Savdo va Foyda", List.of(
                "savdo", "sotuv", "tovar", "magazin", "do'kon", "foyda", "tushum", "daromad"
        ));
        INCOME_KEYWORDS.put("Oylik va Ish haqi", List.of(
                "oylik", "ish haqi", "avans", "ishdan", "maosh", "gonorar"
        ));
        INCOME_KEYWORDS.put("Xizmat ko‘rsatish", List.of(
                "xizmat", "montaj", "remont", "usta", "tuzatish", "montajdan", "klient to'ladi", "klient"
        ));
        INCOME_KEYWORDS.put("Boshqa", List.of(
                "naqd", "qo'lga", "perevod", "o'tkazma", "karta", "click", "payme", "boshqa"
        ));
    }

    public TransactionType detectIntent(String text) {
        if (text == null || text.isBlank()) {
            return TransactionType.EXPENSE;
        }
        String lower = text.toLowerCase()
                .replace("’", "'").replace("‘", "'").replace("`", "'");

        // Specific high-confidence income indicators
        if (lower.contains("ishladim") || lower.contains("daromad") || lower.contains("tushum") ||
            lower.contains("klient") || lower.contains("to'ladi") || lower.contains("toladi")) {
            return TransactionType.INCOME;
        }

        // Check if message has expense-specific items (benzin, obed, ovqat, taxi, etc.)
        for (List<String> words : EXPENSE_KEYWORDS.values()) {
            for (String word : words) {
                if (lower.contains(word)) {
                    return TransactionType.EXPENSE;
                }
            }
        }

        // Check explicit expense words before income (e.g. "foydadan xarajat", "foydadan")
        if (lower.contains("xarajat") || lower.contains("sarf") || lower.contains("ishlatdim") || lower.contains("foydadan")) {
            return TransactionType.EXPENSE;
        }

        // Check income words
        for (String word : INCOME_INTENT_WORDS) {
            if (lower.contains(word)) {
                return TransactionType.INCOME;
            }
        }

        // Check expense words
        for (String word : EXPENSE_INTENT_WORDS) {
            if (lower.contains(word)) {
                return TransactionType.EXPENSE;
            }
        }

        // Default to expense if unclear
        return TransactionType.EXPENSE;
    }

    public Optional<String> matchCategory(String text, TransactionType type) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase()
                .replace("’", "'").replace("‘", "'").replace("`", "'");

        Map<String, List<String>> keywords = (type == TransactionType.INCOME) ? INCOME_KEYWORDS : EXPENSE_KEYWORDS;

        for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.matches(".*\\b" + PatternQuote(keyword) + ".*") || lower.contains(keyword)) {
                    return Optional.of(entry.getKey());
                }
            }
        }

        return Optional.empty();
    }

    private String PatternQuote(String s) {
        return java.util.regex.Pattern.quote(s);
    }
}
