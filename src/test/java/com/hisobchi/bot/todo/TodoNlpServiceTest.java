package com.hisobchi.bot.todo;

import com.hisobchi.bot.ai.service.CategoryMatcher;
import com.hisobchi.bot.ai.service.UzbekAmountParser;
import com.hisobchi.bot.common.util.UzbekDateParser;
import com.hisobchi.bot.todo.dto.ParsedTodo;
import com.hisobchi.bot.todo.entity.TodoPriority;
import com.hisobchi.bot.todo.entity.TodoRecurrenceType;
import com.hisobchi.bot.todo.service.TodoNlpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TodoNlpServiceTest {

    private TodoNlpService nlpService;
    private final ZoneId zoneId = ZoneId.of("Asia/Tashkent");

    @BeforeEach
    void setUp() {
        UzbekAmountParser amountParser = new UzbekAmountParser();
        UzbekDateParser dateParser = new UzbekDateParser();
        CategoryMatcher categoryMatcher = new CategoryMatcher();
        nlpService = new TodoNlpService(amountParser, dateParser, categoryMatcher);
    }

    @Test
    @DisplayName("Should distinguish actual expense from planned task")
    void testDistinguishExpenseVsTask() {
        // Actual expense: "to'ladim"
        assertFalse(nlpService.isTaskMessage("Internetga 150 ming to‘ladim"));
        assertFalse(nlpService.isTaskMessage("300 ming benzinga sarfladim"));

        // Planned task: "eslat", "to'lashim kerak"
        assertTrue(nlpService.isTaskMessage("Ertaga 18:00 da internetga 150 ming to‘lashni eslat"));
        assertTrue(nlpService.isTaskMessage("Internetga 150 ming to‘lashim kerak"));
        assertTrue(nlpService.isTaskMessage("Ertaga 9 da ustaga telefon qilish"));
        assertTrue(nlpService.isTaskMessage("Har oyning 5-kuni ijaraga 2 mln to‘lash"));
    }

    @Test
    @DisplayName("Should parse task with date, time, planned amount and category")
    void testParseFullTask() {
        String text = "Ertaga 18:00 da internetga 150 ming to‘lashni eslat";
        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);

        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();

        LocalDate expectedTomorrow = LocalDate.now(zoneId).plusDays(1);
        assertEquals(expectedTomorrow, parsed.dueDate());
        assertEquals(LocalTime.of(18, 0), parsed.dueTime());
        assertTrue(parsed.hasSpecificTime());
        assertEquals(new BigDecimal("150000"), parsed.plannedAmount());
        assertNotNull(parsed.title());
        assertFalse(parsed.isExpenseIntent());
    }

    @Test
    @DisplayName("Should parse task without date (sanasiz)")
    void testParseDateLessTask() {
        String text = "Ustaga telefon qilish, muhim";
        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);

        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();

        assertNull(parsed.dueDate());
        assertNull(parsed.dueTime());
        assertFalse(parsed.hasSpecificTime());
        assertEquals(TodoPriority.HIGH, parsed.priority());
    }

    @Test
    @DisplayName("Should parse recurring monthly task")
    void testParseMonthlyRecurrence() {
        String text = "Har oyning 5-kuni ijaraga 2 mln to‘lash";
        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);

        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();

        assertEquals(TodoRecurrenceType.MONTHLY, parsed.recurrenceType());
        assertEquals(5, parsed.recurrenceDayOfMonth());
        assertEquals(new BigDecimal("2000000"), parsed.plannedAmount());
    }

    @Test
    @DisplayName("Should parse multi-line tasks list")
    void testParseMultiTasks() {
        String text = """
                1. Ertaga 9 da ustaga telefon qilish
                2. Indinga mahsulot olish, taxminan 300 ming
                3. Juma kuni hisobot topshirish
                """;

        List<ParsedTodo> list = nlpService.parseMultiOrSingle(text, zoneId);
        assertEquals(3, list.size());
        assertEquals(new BigDecimal("300000"), list.get(1).plannedAmount());
    }

    @Test
    @DisplayName("Should parse Russian and Cyrillic tasks")
    void testParseRussianAndCyrillic() {
        String ruText = "Напомни завтра в 18:00 оплатить интернет 150 тысяч";
        Optional<ParsedTodo> optRu = nlpService.parseSingle(ruText, zoneId);
        assertTrue(optRu.isPresent());
        assertEquals(LocalTime.of(18, 0), optRu.get().dueTime());
        assertEquals(new BigDecimal("150000"), optRu.get().plannedAmount());

        String cyrillicText = "Эртага 18:00 да интернетга 150 минг тўлашни эслат";
        Optional<ParsedTodo> optCyr = nlpService.parseSingle(cyrillicText, zoneId);
        assertTrue(optCyr.isPresent());
        assertEquals(LocalTime.of(18, 0), optCyr.get().dueTime());
    }
}
