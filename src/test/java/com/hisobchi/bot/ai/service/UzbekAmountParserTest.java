package com.hisobchi.bot.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UzbekAmountParserTest {

    private UzbekAmountParser parser;

    @BeforeEach
    void setUp() {
        parser = new UzbekAmountParser();
    }

    @Test
    @DisplayName("Parse '15 ming' -> 15000")
    void test15Ming() {
        Optional<BigDecimal> result = parser.parse("15 ming");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("15000"), result.get());
    }

    @Test
    @DisplayName("Parse '15 ming so‘m' -> 15000")
    void test15MingSom() {
        Optional<BigDecimal> result = parser.parse("15 ming so‘m");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("15000"), result.get());
    }

    @Test
    @DisplayName("Parse '100 ming' -> 100000")
    void test100Ming() {
        Optional<BigDecimal> result = parser.parse("100 ming");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("100000"), result.get());
    }

    @Test
    @DisplayName("Parse '1 million' -> 1000000")
    void test1Million() {
        Optional<BigDecimal> result = parser.parse("1 million");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1000000"), result.get());
    }

    @Test
    @DisplayName("Parse '1 mln 250 ming' -> 1250000")
    void test1Mln250Ming() {
        Optional<BigDecimal> result = parser.parse("1 mln 250 ming");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1250000"), result.get());
    }

    @Test
    @DisplayName("Parse 'ikki million' -> 2000000")
    void testIkkiMillion() {
        Optional<BigDecimal> result = parser.parse("ikki million");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("2000000"), result.get());
    }

    @Test
    @DisplayName("Parse '2 yarim million' -> 2500000")
    void test2YarimMillion() {
        Optional<BigDecimal> result = parser.parse("2 yarim million");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("2500000"), result.get());
    }

    @Test
    @DisplayName("Parse '1.2 million' -> 1200000")
    void test1Point2Million() {
        Optional<BigDecimal> result = parser.parse("1.2 million");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1200000"), result.get());
    }

    @Test
    @DisplayName("Parse '15k' -> 15000")
    void test15k() {
        Optional<BigDecimal> result = parser.parse("15k");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("15000"), result.get());
    }

    @Test
    @DisplayName("Parse plain digits '15000' -> 15000")
    void test15000Digits() {
        Optional<BigDecimal> result = parser.parse("15000");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("15000"), result.get());
    }

    @Test
    @DisplayName("Parse space-separated digits '15 000' -> 15000")
    void testSpaceDigits() {
        Optional<BigDecimal> result = parser.parse("15 000");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("15000"), result.get());
    }

    @Test
    @DisplayName("Parse words 'o‘n besh ming' -> 15000")
    void testOnBeshMing() {
        Optional<BigDecimal> result = parser.parse("o‘n besh ming");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("15000"), result.get());
    }

    @Test
    @DisplayName("Parse colloquial '50 min zaprafka' -> 50000")
    void test50MinZaprafka() {
        Optional<BigDecimal> result = parser.parse("50 min zaprafka");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("50000"), result.get());
    }

    @Test
    @DisplayName("Parse acoustic artifact 'ilmiy zapravka' -> 50000")
    void testIlmiyZapravka() {
        Optional<BigDecimal> result = parser.parse("ilmiy zapravka");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("50000"), result.get());
    }

    @Test
    @DisplayName("Parse dialectal 'elli min zaprafka' -> 50000")
    void testElliMinZaprafka() {
        Optional<BigDecimal> result = parser.parse("elli min zaprafka");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("50000"), result.get());
    }

    @Test
    @DisplayName("Duration text 'Oxirgi 7 kun' must NOT be parsed as amount")
    void testOxirgi7KunNotAmount() {
        Optional<BigDecimal> result = parser.parse("Oxirgi 7 kun");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Duration text '7 kun' must NOT be parsed as 7k/7000")
    void test7KunNotAmount() {
        Optional<BigDecimal> result = parser.parse("7 kun");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Duration text '14 kun' must NOT be parsed as amount")
    void test14KunNotAmount() {
        Optional<BigDecimal> result = parser.parse("14 kun");
        assertTrue(result.isEmpty());
    }
}
