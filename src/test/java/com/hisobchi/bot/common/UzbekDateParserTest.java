package com.hisobchi.bot.common;

import com.hisobchi.bot.common.util.UzbekDateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UzbekDateParserTest {

    private UzbekDateParser parser;
    private final ZoneId zoneId = ZoneId.of("Asia/Tashkent");

    @BeforeEach
    void setUp() {
        parser = new UzbekDateParser();
    }

    @Test
    @DisplayName("Parse 'ertaga' -> today + 1")
    void testErtaga() {
        Optional<LocalDate> date = parser.parseDate("ertaga qaytaraman", zoneId);
        assertTrue(date.isPresent());
        assertEquals(LocalDate.now(zoneId).plusDays(1), date.get());
    }

    @Test
    @DisplayName("Parse '3 kundan keyin' -> today + 3")
    void test3KundanKeyin() {
        Optional<LocalDate> date = parser.parseDate("3 kundan keyin beraman", zoneId);
        assertTrue(date.isPresent());
        assertEquals(LocalDate.now(zoneId).plusDays(3), date.get());
    }

    @Test
    @DisplayName("Parse 'keyingi hafta' -> today + 7")
    void testKeyingiHafta() {
        Optional<LocalDate> date = parser.parseDate("keyingi haftaga berishim kerak", zoneId);
        assertTrue(date.isPresent());
        assertEquals(LocalDate.now(zoneId).plusWeeks(1), date.get());
    }

    @Test
    @DisplayName("Parse numeric date '15.10.2026'")
    void testNumericDate() {
        Optional<LocalDate> date = parser.parseDate("15.10.2026 da qaytaradi", zoneId);
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2026, 10, 15), date.get());
    }
}
