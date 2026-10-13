package com.hisobchi.bot.transaction.service;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.entity.CategoryType;
import com.hisobchi.bot.summary.service.DailySummaryService;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardMarkup;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionCategoryEditTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionDraftService draftService;

    @Mock
    private DailySummaryService dailySummaryService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Category fuelCat;
    private Category foodCat;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();

        fuelCat = Category.builder()
                .id(10L)
                .name("Yoqilg‘i")
                .emoji("⛽")
                .type(CategoryType.EXPENSE)
                .build();

        foodCat = Category.builder()
                .id(20L)
                .name("Ovqat")
                .emoji("🍽")
                .type(CategoryType.EXPENSE)
                .build();

        tx = Transaction.builder()
                .id(100L)
                .user(user)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100000"))
                .currency("UZS")
                .category(fuelCat)
                .transactionDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Har bir xarajat kategoriyasini o‘zgartirish muvaffaqiyatli ishlaydi")
    void testUpdateTransactionCategory() {
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Category o'zgartiriladi (Yoqilg'idan Ovqatga)
        TransactionDto updated = transactionService.updateTransaction(
                100L, user.getId(), null, foodCat, null, null);

        assertNotNull(updated);
        assertEquals(20L, updated.categoryId());
        assertEquals("Ovqat", updated.categoryName());
        assertEquals("🍽", updated.categoryEmoji());
        assertEquals(new BigDecimal("100000"), updated.amount());
    }

    @Test
    @DisplayName("Har bir xarajat summasi va izohini tahrirlash")
    void testUpdateTransactionAmountAndDescription() {
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDto updated = transactionService.updateTransaction(
                100L, user.getId(), new BigDecimal("120000"), null, "LPG gaz to‘ldirildi", null);

        assertNotNull(updated);
        assertEquals(new BigDecimal("120000"), updated.amount());
        assertEquals("LPG gaz to‘ldirildi", updated.description());
        assertEquals("Yoqilg‘i", updated.categoryName());
    }

    @Test
    @DisplayName("Inline klaviatura orqali kategoriyalar tanlovi to‘g‘ri shakllanadi")
    void testTransactionCategorySelectionKeyboard() {
        InlineKeyboardFactory factory = new InlineKeyboardFactory();
        List<Category> categories = List.of(fuelCat, foodCat);

        InlineKeyboardMarkup markup = factory.getTransactionCategorySelectionKeyboard(100L, categories);

        assertNotNull(markup);
        assertFalse(markup.getInlineKeyboard().isEmpty());

        // Callback datalar tekshiriladi
        String callback0 = markup.getInlineKeyboard().get(0).get(0).getCallbackData();
        assertEquals("tx:set_cat:100:10", callback0);

        String callback1 = markup.getInlineKeyboard().get(0).get(1).getCallbackData();
        assertEquals("tx:set_cat:100:20", callback1);

        // Yangi kategoriya tugmasi tekshiriladi
        String addCatCallback = markup.getInlineKeyboard().get(1).get(0).getCallbackData();
        assertEquals("tx:add_cat:100", addCatCallback);
        assertEquals("➕ Yangi kategoriya", markup.getInlineKeyboard().get(1).get(0).getText());
    }

    @Test
    @DisplayName("Draft uchun kategoriya tanlash klaviaturasida yangi kategoriya tugmasi mavjud")
    void testDraftCategorySelectionKeyboard() {
        InlineKeyboardFactory factory = new InlineKeyboardFactory();
        List<Category> categories = List.of(fuelCat, foodCat);

        InlineKeyboardMarkup markup = factory.getCategorySelectionKeyboard(55L, categories);

        assertNotNull(markup);
        assertEquals("draft:set_cat:55:10", markup.getInlineKeyboard().get(0).get(0).getCallbackData());
        assertEquals("draft:add_cat:55", markup.getInlineKeyboard().get(1).get(0).getCallbackData());
        assertEquals("➕ Yangi kategoriya", markup.getInlineKeyboard().get(1).get(0).getText());
        assertEquals("draft:cancel:55", markup.getInlineKeyboard().get(2).get(0).getCallbackData());
    }

    @Test
    @DisplayName("Operatsiyani saqlagandan so‘ng kategoriya o‘zgartirish, o‘chirish va asosiy menyu tugmasi mavjud bo‘ladi")
    void testSavedTransactionKeyboard() {
        InlineKeyboardFactory factory = new InlineKeyboardFactory();
        InlineKeyboardMarkup markup = factory.getSavedTransactionKeyboard(100L);

        assertNotNull(markup);
        assertEquals(3, markup.getInlineKeyboard().size());
        assertEquals("tx:edit_field:100:cat", markup.getInlineKeyboard().get(0).get(0).getCallbackData());
        assertEquals("📂 Kategoriyani o‘zgartirish", markup.getInlineKeyboard().get(0).get(0).getText());
        assertEquals("tx:delete_ask:100", markup.getInlineKeyboard().get(1).get(0).getCallbackData());
        assertEquals("🗑 O‘chirish", markup.getInlineKeyboard().get(1).get(0).getText());
        assertEquals("tx:list_expenses_today", markup.getInlineKeyboard().get(1).get(1).getCallbackData());
        assertEquals("🧾 Bugungi xarajatlar", markup.getInlineKeyboard().get(1).get(1).getText());
        assertEquals("menu:main", markup.getInlineKeyboard().get(2).get(0).getCallbackData());
        assertEquals("🏠 Asosiy menyuga qaytish", markup.getInlineKeyboard().get(2).get(0).getText());
    }

    @Test
    @DisplayName("Qo‘shilgan xarajatlar alohida-alohida va o‘chirish tugmasi bilan chiqadi")
    void testExpensesListKeyboardAndMessage() {
        InlineKeyboardFactory factory = new InlineKeyboardFactory();
        LocalDate today = LocalDate.of(2026, 10, 13);
        Category cat1 = Category.builder().id(1L).name("Uy").emoji("🏠").type(CategoryType.EXPENSE).build();
        Category cat2 = Category.builder().id(2L).name("Tushlik").emoji("🍔").type(CategoryType.EXPENSE).build();

        Transaction tx1 = Transaction.builder()
                .id(101L)
                .amount(new BigDecimal("50000"))
                .category(cat1)
                .type(TransactionType.EXPENSE)
                .transactionDate(today)
                .createdAt(java.time.Instant.now())
                .build();
        Transaction tx2 = Transaction.builder()
                .id(102L)
                .amount(new BigDecimal("30000"))
                .category(cat2)
                .type(TransactionType.EXPENSE)
                .transactionDate(today)
                .createdAt(java.time.Instant.now())
                .build();

        List<Transaction> list = List.of(tx1, tx2);

        InlineKeyboardMarkup markup = factory.getExpensesListKeyboard(list, today);
        assertNotNull(markup);
        // 2 expense rows + 1 bottom action row = 3 rows
        assertEquals(3, markup.getInlineKeyboard().size());

        // Row 1: item 1 + delete button
        assertEquals("tx:detail:101", markup.getInlineKeyboard().get(0).get(0).getCallbackData());
        assertEquals("tx:delete_ask:101", markup.getInlineKeyboard().get(0).get(1).getCallbackData());
        assertEquals("🗑 O‘chirish", markup.getInlineKeyboard().get(0).get(1).getText());

        // Row 2: item 2 + delete button
        assertEquals("tx:detail:102", markup.getInlineKeyboard().get(1).get(0).getCallbackData());
        assertEquals("tx:delete_ask:102", markup.getInlineKeyboard().get(1).get(1).getCallbackData());
        assertEquals("🗑 O‘chirish", markup.getInlineKeyboard().get(1).get(1).getText());

        // Message text check
        String msg = com.hisobchi.bot.telegram.handler.BotMessageBuilder.buildExpensesListMessage(today, list, "Asia/Tashkent");
        assertTrue(msg.contains("Uy"));
        assertTrue(msg.contains("50 000"));
        assertTrue(msg.contains("Tushlik"));
        assertTrue(msg.contains("30 000"));
        assertTrue(msg.contains("Jami xarajatlar"));
    }
}
