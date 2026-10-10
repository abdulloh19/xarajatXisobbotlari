package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserState;
import com.hisobchi.bot.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryHandlerTest {

    @Mock
    private TelegramApiClient apiClient;
    @Mock
    private UserService userService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private com.hisobchi.bot.profit.service.DailyProfitService dailyProfitService;
    @Mock
    private com.hisobchi.bot.debt.repository.DebtRepository debtRepository;
    @Mock
    private com.hisobchi.bot.debt.repository.DebtPaymentRepository debtPaymentRepository;

    @Spy
    private ReplyKeyboardFactory replyKeyboardFactory = new ReplyKeyboardFactory();
    @Spy
    private InlineKeyboardFactory inlineKeyboardFactory = new InlineKeyboardFactory();

    @InjectMocks
    private TextMessageHandler textMessageHandler;

    private User user;
    private final Long chatId = 12345L;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .telegramId(12345L)
                .timezone("Asia/Tashkent")
                .state(UserState.IDLE)
                .build();
    }

    @Test
    @DisplayName("Build day transactions history detailed returns empty message when no txs")
    void testBuildDayTransactionsHistoryEmpty() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        String msg = BotMessageBuilder.buildDayTransactionsHistoryDetailed(date, List.of(), "Asia/Tashkent");
        assertTrue(msg.contains("topilmadi"));
    }

    @Test
    @DisplayName("Build day transactions history detailed formats transactions with totals")
    void testBuildDayTransactionsHistoryDetailed() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        Category catFood = Category.builder().id(1L).name("Ovqat").emoji("🍽").build();
        Category catTransport = Category.builder().id(2L).name("Transport").emoji("🚖").build();

        Transaction tx1 = Transaction.builder()
                .id(101L)
                .category(catFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("124000"))
                .createdAt(Instant.parse("2026-10-04T10:00:00Z"))
                .transactionDate(date)
                .build();

        Transaction tx2 = Transaction.builder()
                .id(102L)
                .category(catTransport)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("300000"))
                .createdAt(Instant.parse("2026-10-04T12:00:00Z"))
                .transactionDate(date)
                .build();

        String msg = BotMessageBuilder.buildDayTransactionsHistoryDetailed(date, List.of(tx1, tx2), "Asia/Tashkent");
        assertTrue(msg.contains("OPERATSIYALAR TARIXI"));
        assertTrue(msg.contains("424 000"));
        assertTrue(msg.contains("Ovqat"));
        assertTrue(msg.contains("Transport"));
        assertTrue(msg.contains("2 ta"));
    }

    @Test
    @DisplayName("Build period transactions history groups by date and shows totals")
    void testBuildPeriodTransactionsHistory() {
        LocalDate start = LocalDate.of(2026, 9, 28);
        LocalDate end = LocalDate.of(2026, 10, 4);

        Category catFuel = Category.builder().id(3L).name("Yoqilg‘i").emoji("⛽").build();

        Transaction tx = Transaction.builder()
                .id(103L)
                .category(catFuel)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("250000"))
                .createdAt(Instant.parse("2026-10-03T08:00:00Z"))
                .transactionDate(LocalDate.of(2026, 10, 3))
                .build();

        String msg = BotMessageBuilder.buildPeriodTransactionsHistory("Oxirgi 7 kunlik", start, end, List.of(tx), "Asia/Tashkent");
        assertTrue(msg.contains("OXIRGI 7 KUNLIK TARIXI"));
        assertTrue(msg.contains("250 000"));
        assertTrue(msg.contains("Yoqilg‘i"));
        assertTrue(msg.contains("3-oktabr 2026"));
    }

    @Test
    @DisplayName("Selecting '📜 Tarix' sets user active menu to HISTORY and sends history menu")
    void testSelectHistoryMenu() {
        Message msg = createTextMessage("📜 Tarix");
        textMessageHandler.handle(user, msg);

        assertEquals("HISTORY", textMessageHandler.getUserActiveMenu(user.getId()));
        verify(apiClient).sendMessage(eq(chatId), contains("Tarix bo‘limi"), any(ReplyKeyboardMarkup.class), eq("HTML"));
    }

    @Test
    @DisplayName("Clicking '📅 Oxirgi 7 kun' from Tarix queries period transactions and sends history")
    void testClickOxirgi7KunHistory() {
        textMessageHandler.setUserActiveMenu(user.getId(), "HISTORY");
        when(transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                eq(user.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        Message msg = createTextMessage("📅 Oxirgi 7 kun");
        textMessageHandler.handle(user, msg);

        verify(transactionRepository).findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                eq(user.getId()), any(LocalDate.class), any(LocalDate.class));
        verify(apiClient).sendMessage(eq(chatId), contains("Oxirgi 7 kunlik"), any(InlineKeyboardMarkup.class), eq("HTML"));
    }

    @Test
    @DisplayName("Clicking '📅 Kecha' queries yesterday's transactions and sends day history")
    void testClickKechaHistory() {
        when(transactionRepository.findByUserIdAndTransactionDateOrderByCreatedAtAsc(eq(user.getId()), any(LocalDate.class)))
                .thenReturn(List.of());

        Message msg = createTextMessage("📅 Kecha");
        textMessageHandler.handle(user, msg);

        verify(transactionRepository).findByUserIdAndTransactionDateOrderByCreatedAtAsc(eq(user.getId()), any(LocalDate.class));
        verify(apiClient).sendMessage(eq(chatId), anyString(), any(InlineKeyboardMarkup.class), eq("HTML"));
    }

    @Test
    @DisplayName("Clicking '⬅️ Asosiy menyu' clears userActiveMenu")
    void testCancelClearsUserActiveMenu() {
        textMessageHandler.setUserActiveMenu(user.getId(), "HISTORY");
        Message msg = createTextMessage("⬅️ Asosiy menyu");
        textMessageHandler.handle(user, msg);

        assertNull(textMessageHandler.getUserActiveMenu(user.getId()));
    }

    @Test
    @DisplayName("Clicking '📜 Bugun' directly queries today transactions, debts, payments, profit")
    void testClickDirectBugunHistory() {
        when(transactionRepository.findByUserIdAndTransactionDateOrderByCreatedAtAsc(eq(user.getId()), any(LocalDate.class)))
                .thenReturn(List.of());

        Message msg = createTextMessage("📜 Bugun");
        textMessageHandler.handle(user, msg);

        verify(transactionRepository).findByUserIdAndTransactionDateOrderByCreatedAtAsc(eq(user.getId()), any(LocalDate.class));
        verify(apiClient).sendMessage(eq(chatId), anyString(), any(InlineKeyboardMarkup.class), eq("HTML"));
    }

    @Test
    @DisplayName("Clicking '📜 Tarix' while in WAITING_EXPENSE_AMOUNT breaks out of state and opens history menu")
    void testHistoryMenuBreaksOutOfState() {
        user.setState(UserState.WAITING_EXPENSE_AMOUNT);
        Message msg = createTextMessage("📜 Tarix");
        textMessageHandler.handle(user, msg);

        verify(userService).updateState(eq(user.getTelegramId()), eq(UserState.IDLE));
        verify(apiClient).sendMessage(eq(chatId), contains("Tarix bo‘limi"), any(ReplyKeyboardMarkup.class), eq("HTML"));
    }

    @Test
    @DisplayName("Transactions list keyboard generates buttons with detail callback and back to history")
    void testTransactionsListKeyboard() {
        Category cat = Category.builder().id(1L).name("Tushlik").emoji("🍽").build();
        Transaction tx = Transaction.builder()
                .id(999L)
                .category(cat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50000"))
                .build();

        InlineKeyboardMarkup markup = inlineKeyboardFactory.getTransactionsListKeyboard(List.of(tx), 10);
        assertNotNull(markup);
        assertEquals(2, markup.getInlineKeyboard().size()); // 1 for item + 1 for back

        InlineKeyboardButton itemBtn = markup.getInlineKeyboard().get(0).get(0);
        assertEquals("tx:detail:999", itemBtn.getCallbackData());
        assertTrue(itemBtn.getText().contains("Tushlik"));
        assertTrue(itemBtn.getText().contains("50 000"));

        InlineKeyboardButton backBtn = markup.getInlineKeyboard().get(1).get(0);
        assertEquals("history:back", backBtn.getCallbackData());
    }

    private Message createTextMessage(String text) {
        return Message.builder()
                .messageId(1)
                .chat(Chat.builder().id(chatId).type("private").build())
                .from(com.hisobchi.bot.telegram.client.model.TelegramModels.User.builder()
                        .id(user.getTelegramId())
                        .isBot(false)
                        .firstName("Test")
                        .build())
                .text(text)
                .build();
    }
}
