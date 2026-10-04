package com.hisobchi.bot.debt;

import com.hisobchi.bot.ai.dto.ParsedDebt;
import com.hisobchi.bot.ai.service.DebtNlpService;
import com.hisobchi.bot.ai.service.UzbekAmountParser;
import com.hisobchi.bot.common.util.UzbekDateParser;
import com.hisobchi.bot.debt.entity.DebtType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebtNlpServiceTest {

    private DebtNlpService debtNlpService;
    private final ZoneId zoneId = ZoneId.of("Asia/Tashkent");

    @BeforeEach
    void setUp() {
        UzbekAmountParser amountParser = new UzbekAmountParser();
        UzbekDateParser dateParser = new UzbekDateParser();
        debtNlpService = new DebtNlpService(amountParser, dateParser);
    }

    @Test
    @DisplayName("Parse '2 million 500 ming Rustam akadan oldim, keyingi haftaga berishim kerak'")
    void testParseBorrowedDebt() {
        String text = "2 million 500 ming Rustam akadan oldim, keyingi haftaga berishim kerak";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(DebtType.BORROWED, debt.type());
        assertEquals(new BigDecimal("2500000"), debt.amount());
        assertEquals("Rustam Aka", debt.personName());
        assertEquals(LocalDate.now(zoneId).plusWeeks(1), debt.dueDate());
    }

    @Test
    @DisplayName("Parse 'Sherzodga 3 million qarz berdim, keyingi dushanba qaytaradi'")
    void testParseLentDebt() {
        String text = "Sherzodga 3 million qarz berdim, keyingi dushanba qaytaradi";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(DebtType.LENT, debt.type());
        assertEquals(new BigDecimal("3000000"), debt.amount());
        assertEquals("Sherzod", debt.personName());
    }

    @Test
    @DisplayName("Parse 'Akmal akadan 500 ming qarz oldim, 3 kundan keyin beraman'")
    void testParseAkmalAka() {
        String text = "Akmal akadan 500 ming qarz oldim, 3 kundan keyin beraman";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(DebtType.BORROWED, debt.type());
        assertEquals(new BigDecimal("500000"), debt.amount());
        assertEquals("Akmal Aka", debt.personName());
        assertEquals(LocalDate.now(zoneId).plusDays(3), debt.dueDate());
    }

    @Test
    @DisplayName("Parse full repayment: 'Rustam akaga qarzimni hammasini to''ladim'")
    void testParseRepayFull() {
        String text = "Rustam akaga qarzimni hammasini to'ladim";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(com.hisobchi.bot.ai.dto.DebtIntent.REPAY_FULL, debt.intent());
        assertEquals("Rustam Aka", debt.personName());
        assertEquals(DebtType.BORROWED, debt.type());
    }

    @Test
    @DisplayName("Parse full return: 'Javlon hamma qarzini qaytardi'")
    void testParseReturnFull() {
        String text = "Javlon hamma qarzini qaytardi";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(com.hisobchi.bot.ai.dto.DebtIntent.RETURN_FULL, debt.intent());
        assertEquals("Javlon", debt.personName());
        assertEquals(DebtType.LENT, debt.type());
    }

    @Test
    @DisplayName("Parse partial repayment: 'Rustam akaga qarzimdan 500 ming berdim'")
    void testParseRepayPartial() {
        String text = "Rustam akaga qarzimdan 500 ming berdim";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(com.hisobchi.bot.ai.dto.DebtIntent.REPAY_PARTIAL, debt.intent());
        assertEquals("Rustam Aka", debt.personName());
        assertEquals(new BigDecimal("500000"), debt.amount());
    }

    @Test
    @DisplayName("Parse partial return: 'Javlon 600 ming qarz qaytardi'")
    void testParseReturnPartial() {
        String text = "Javlon 600 ming qarz qaytardi";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(com.hisobchi.bot.ai.dto.DebtIntent.RETURN_PARTIAL, debt.intent());
        assertEquals("Javlon", debt.personName());
        assertEquals(new BigDecimal("600000"), debt.amount());
    }

    @Test
    @DisplayName("Parse '250min qarzman' -> BORROWED, 250 000, missingPerson=true")
    void testParseQarzmanWithoutPerson() {
        String text = "250min qarzman";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(DebtType.BORROWED, debt.type());
        assertEquals(new BigDecimal("250000"), debt.amount());
        assertTrue(debt.missingPerson());
    }

    @Test
    @DisplayName("Parse 'moydan 250 ming qarzman' -> BORROWED, 250 000, person='Moy'")
    void testParseMoydanQarzman() {
        String text = "moydan 250 ming qarzman";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(DebtType.BORROWED, debt.type());
        assertEquals(new BigDecimal("250000"), debt.amount());
        assertEquals("Moy", debt.personName());
        org.junit.jupiter.api.Assertions.assertFalse(debt.missingPerson());
    }

    @Test
    @DisplayName("Parse '250 min moydan qarz olganman' -> BORROWED, 250 000, person='Moy'")
    void testParseMoydanQarzOlganman() {
        String text = "250 min moydan qarz olganman";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(DebtType.BORROWED, debt.type());
        assertEquals(new BigDecimal("250000"), debt.amount());
        assertEquals("Moy", debt.personName());
        org.junit.jupiter.api.Assertions.assertFalse(debt.missingPerson());
    }

    @Test
    @DisplayName("Parse 'zapravkadan 100 ming qarzman' -> BORROWED, 100 000, person='Zapravka'")
    void testParseZapravkadanQarzman() {
        String text = "zapravkadan 100 ming qarzman";
        Optional<ParsedDebt> result = debtNlpService.parse(text, zoneId);

        assertTrue(result.isPresent());
        ParsedDebt debt = result.get();
        assertEquals(DebtType.BORROWED, debt.type());
        assertEquals(new BigDecimal("100000"), debt.amount());
        assertEquals("Zapravka", debt.personName());
        org.junit.jupiter.api.Assertions.assertFalse(debt.missingPerson());
    }
}
