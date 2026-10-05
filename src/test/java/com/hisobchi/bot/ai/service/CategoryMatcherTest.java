package com.hisobchi.bot.ai.service;

import com.hisobchi.bot.transaction.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMatcherTest {

    private CategoryMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new CategoryMatcher();
    }

    @Test
    @DisplayName("Match category: 'obedga 15 ming' -> Ovqat")
    void testMatchOvqat() {
        Optional<String> category = matcher.matchCategory("obedga 15 ming", TransactionType.EXPENSE);
        assertTrue(category.isPresent());
        assertEquals("Ovqat", category.get());
    }

    @Test
    @DisplayName("Match category: 'benzinga 300 ming' -> Yoqilg‘i")
    void testMatchYoqilgi() {
        Optional<String> category = matcher.matchCategory("benzinga 300 ming", TransactionType.EXPENSE);
        assertTrue(category.isPresent());
        assertEquals("Yoqilg‘i", category.get());
    }

    @Test
    @DisplayName("Match category: 'mis trubaga 500 ming' -> Material")
    void testMatchMaterial() {
        Optional<String> category = matcher.matchCategory("mis trubaga 500 ming", TransactionType.EXPENSE);
        assertTrue(category.isPresent());
        assertEquals("Material", category.get());
    }

    @Test
    @DisplayName("Match category: 'usta haqqiga 800 ming' -> Ish")
    void testMatchIsh() {
        Optional<String> category = matcher.matchCategory("usta haqqiga 800 ming", TransactionType.EXPENSE);
        assertTrue(category.isPresent());
        assertEquals("Ish", category.get());
    }

    @Test
    @DisplayName("Match category: 'taxi 25 ming' -> Transport")
    void testMatchTransport() {
        Optional<String> category = matcher.matchCategory("taxi 25 ming", TransactionType.EXPENSE);
        assertTrue(category.isPresent());
        assertEquals("Transport", category.get());
    }

    @Test
    @DisplayName("Intent: '2 million ishladim' -> INCOME")
    void testIntentIncome() {
        TransactionType intent = matcher.detectIntent("2 million ishladim");
        assertEquals(TransactionType.INCOME, intent);
    }

    @Test
    @DisplayName("Intent: '15 ming sarfladim' -> EXPENSE")
    void testIntentExpense() {
        TransactionType intent = matcher.detectIntent("15 ming sarfladim");
        assertEquals(TransactionType.EXPENSE, intent);
    }

    @Test
    @DisplayName("Intent: 'montajdan 3 million oldim' -> INCOME")
    void testIntentMontajIncome() {
        TransactionType intent = matcher.detectIntent("montajdan 3 million oldim");
        assertEquals(TransactionType.INCOME, intent);
    }

    @Test
    @DisplayName("Intent: 'benzinga 200 ming ketdi' -> EXPENSE")
    void testIntentBenzinExpense() {
        TransactionType intent = matcher.detectIntent("benzinga 200 ming ketdi");
        assertEquals(TransactionType.EXPENSE, intent);
    }

    @Test
    @DisplayName("Intent: 'Foydadan 5 min xarajatga qo‘sh' -> EXPENSE")
    void testIntentFoydadanXarajatExpense() {
        TransactionType intent = matcher.detectIntent("Foydadan 5 min xarajatga qo‘sh");
        assertEquals(TransactionType.EXPENSE, intent);
    }

    @Test
    @DisplayName("Intent: 'foydadan 20 min' -> EXPENSE")
    void testIntentFoydadanExpense() {
        TransactionType intent = matcher.detectIntent("foydadan 20 min");
        assertEquals(TransactionType.EXPENSE, intent);
    }
}
