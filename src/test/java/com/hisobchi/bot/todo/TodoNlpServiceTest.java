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

    @Test
    @DisplayName("Voice task 'soat 12-da miting bor' must be detected with 12:00 and NO planned amount")
    void testVoiceMeetingTask() {
        String text = "soat 12-da miting bor";
        assertTrue(nlpService.isTaskMessage(text));

        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);
        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();
        assertEquals(LocalTime.of(12, 0), parsed.dueTime());
        assertNull(parsed.plannedAmount(), "Hour 12 must NOT be parsed as 12 so'm planned amount!");
    }

    @Test
    @DisplayName("Task 'Bugun ertalab soat 9 da ingiliz tili darsimni qilish kere' must NOT have 9 so'm amount")
    void testTaskWithHourNoAmount() {
        String text = "Bugun ertalab soat 9 da ingiliz tili darsimni qilish kere";
        assertTrue(nlpService.isTaskMessage(text));

        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);
        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();
        assertEquals(LocalTime.of(9, 0), parsed.dueTime());
        assertEquals(LocalDate.now(zoneId), parsed.dueDate());
        assertNull(parsed.plannedAmount(), "Hour 9 must NOT be parsed as 9 so'm planned amount!");
    }

    @Test
    @DisplayName("Task 'soat 9 ga vazifa belgilash' must have time 09:00 and NO amount")
    void testTaskSoat9GaWithoutAmount() {
        String text = "soat 9 ga vazifa belgilash";
        assertTrue(nlpService.isTaskMessage(text));

        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);
        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();
        assertEquals(LocalTime.of(9, 0), parsed.dueTime());
        assertNull(parsed.plannedAmount());
    }

    @Test
    @DisplayName("Financial task 'soat 9 da ustaga 50 ming berish kere' has both time 09:00 and 50000 amount")
    void testFinancialTaskWithTimeAndAmount() {
        String text = "soat 9 da ustaga 50 ming berish kere";
        assertTrue(nlpService.isTaskMessage(text));

        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);
        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();
        assertEquals(LocalTime.of(9, 0), parsed.dueTime());
        assertEquals(new BigDecimal("50000"), parsed.plannedAmount());
    }

    @Test
    @DisplayName("User exact test: 'Soat 7 da oyin bor' must have time and NO planned amount (not 7 so'm)")
    void testGameAt7WithoutAmount() {
        String text = "Soat 7 da oyin bor";
        assertTrue(nlpService.isTaskMessage(text), "'Soat 7 da oyin bor' should be recognized as a task");

        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);
        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();
        assertNotNull(parsed.dueTime());
        assertTrue(parsed.dueTime().equals(LocalTime.of(7, 0)) || parsed.dueTime().equals(LocalTime.of(19, 0)));
        assertNull(parsed.plannedAmount(), "Must not have planned amount of 7 so'm!");
    }

    @Test
    @DisplayName("User voice exact test: 'soati 7da o'yin bor' must be recognized and have NO planned amount")
    void testVoiceGameAt7() {
        String text = "soati 7da o'yin bor";
        assertTrue(nlpService.isTaskMessage(text), "'soati 7da o'yin bor' should be recognized as a task");

        Optional<ParsedTodo> opt = nlpService.parseSingle(text, zoneId);
        assertTrue(opt.isPresent());
        ParsedTodo parsed = opt.get();
        assertNotNull(parsed.dueTime());
        assertTrue(parsed.dueTime().equals(LocalTime.of(7, 0)) || parsed.dueTime().equals(LocalTime.of(19, 0)));
        assertNull(parsed.plannedAmount(), "Must not have planned amount of 7 so'm!");
    }
}
