package com.hisobchi.bot.common.formatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfitCalculationTest {

    @Test
    @DisplayName("Profit calculation formula: grossIncome = enteredProfit + totalExpense")
    void testGrossIncomeFormula() {
        BigDecimal enteredProfit = new BigDecimal("700000");
        BigDecimal totalExpense = new BigDecimal("125000");
        BigDecimal grossIncome = enteredProfit.add(totalExpense);

        assertEquals(new BigDecimal("825000"), grossIncome);
        assertEquals(new BigDecimal("700000"), enteredProfit);
    }

    @Test
    @DisplayName("MoneyFormatter formatting tests")
    void testMoneyFormatter() {
        assertEquals("15 000 so‘m", MoneyFormatter.format(new BigDecimal("15000")));
        assertEquals("1 250 000 so‘m", MoneyFormatter.format(new BigDecimal("1250000")));
        assertEquals("+2 000 000 so‘m", MoneyFormatter.formatSigned(new BigDecimal("2000000")));
        assertEquals("-25 000 so‘m", MoneyFormatter.formatSigned(new BigDecimal("-25000")));
    }
}
