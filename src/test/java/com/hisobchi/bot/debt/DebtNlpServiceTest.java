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
}
