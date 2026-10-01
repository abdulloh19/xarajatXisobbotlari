package com.hisobchi.bot.common.formatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormatter {

    private static final String DEFAULT_CURRENCY = "so‘m";

    private MoneyFormatter() {
    }

    public static String format(BigDecimal amount) {
        return format(amount, DEFAULT_CURRENCY);
    }

    public static String format(BigDecimal amount, String currency) {
        if (amount == null) {
            return "0 " + (currency != null ? currency : DEFAULT_CURRENCY);
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');

        // Strip trailing zeros if scale exists
        BigDecimal clean = amount.stripTrailingZeros();
        String pattern;
        if (clean.scale() > 0) {
            pattern = "#,##0.00";
        } else {
            pattern = "#,##0";
        }

        DecimalFormat df = new DecimalFormat(pattern, symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);

        String currStr = (currency != null && !currency.isBlank()) ? currency : DEFAULT_CURRENCY;
        return df.format(amount) + " " + currStr;
    }

    public static String formatSigned(BigDecimal amount) {
        return formatSigned(amount, DEFAULT_CURRENCY);
    }

    public static String formatSigned(BigDecimal amount, String currency) {
        if (amount == null) {
            return "0 " + (currency != null ? currency : DEFAULT_CURRENCY);
        }
        String formatted = format(amount.abs(), currency);
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + formatted;
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return "-" + formatted;
        } else {
            return formatted;
        }
    }
}
