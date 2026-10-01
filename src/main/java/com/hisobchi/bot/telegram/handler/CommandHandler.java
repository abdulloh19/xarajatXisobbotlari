package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.common.exception.UnauthorizedAccessException;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.debt.entity.DebtStatus;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.service.TransactionService;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserState;
import com.hisobchi.bot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandHandler {

    private final TelegramApiClient apiClient;
    private final ReplyKeyboardFactory replyKeyboardFactory;
    private final InlineKeyboardFactory inlineKeyboardFactory;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final DebtRepository debtRepository;

    public void handleStart(User user, Long chatId) {
        userService.updateState(user.getTelegramId(), UserState.IDLE);
        categoryService.initDefaultCategoriesIfNone(user);

        String welcome = BotMessageBuilder.buildWelcomeMessage();
        apiClient.sendMessage(chatId, welcome, replyKeyboardFactory.getMainMenu(), "HTML");
    }

    public void handleHelp(User user, Long chatId) {
        String help = """
                💡 <b>Hisobchi Bot bo‘yicha qo‘llanma:</b>

                1. <b>Ovozli xabar:</b>
                Ovozli xabar yuboring, masalan:
                🎙 <i>"15 ming obedga"</i>
                🎙 <i>"250 ming mashinaga benzin oldim"</i>
                🎙 <i>"Rustam akadan 2 million qarz oldim, keyingi haftagacha"</i>

                2. <b>Matn orqali:</b>
                Shunchaki yozing:
                ✍️ <i>"15000 tushlik"</i>
                ✍️ <i>"300 ming benzin"</i>
                ✍️ <i>"Javlonga 500 ming qarz berdim"</i>

                3. <b>Menyu orqali:</b>
                "💸 Xarajat qo‘shish", "💰 Daromad qo‘shish", "💵 Foydani kiritish", "📋 Qarzlar" yoki "📊 Hisobotlar" tugmasini bosing.

                Har bir operatsiya avval tasdiqlash uchun ko‘rsatiladi!
                """;
        apiClient.sendMessage(chatId, help, replyKeyboardFactory.getMainMenu(), "HTML");
    }

    public void handleTransactionDetailCommand(User user, Long chatId, String command) {
        try {
            Long txId = Long.parseLong(command.replace("/tx_", "").trim());
            Transaction tx = transactionService.getByIdAndUser(txId, user.getId());
            TransactionDto dto = transactionService.toDto(tx);

            String message = BotMessageBuilder.buildTransactionDetail(dto, user.getTimezone());
            apiClient.sendMessage(chatId, message, inlineKeyboardFactory.getHistoryItemActionsKeyboard(txId), "HTML");
        } catch (NumberFormatException | EntityNotFoundException e) {
            apiClient.sendMessage(chatId, "⚠️ Operatsiya topilmadi.", replyKeyboardFactory.getMainMenu(), null);
        } catch (UnauthorizedAccessException e) {
            apiClient.sendMessage(chatId, "⛔ Bu operatsiya sizga tegishli emas!", replyKeyboardFactory.getMainMenu(), null);
        }
    }

    public void handleDebtDetailCommand(User user, Long chatId, String command) {
        try {
            String idStr = command.replace("/debt_view_", "").replace("/debt_", "").trim();
            Long debtId = Long.parseLong(idStr);
            var debtOpt = debtRepository.findByIdAndUserId(debtId, user.getId());
            if (debtOpt.isEmpty()) {
                apiClient.sendMessage(chatId, "⚠️ Qarz topilmadi.", replyKeyboardFactory.getMainMenu(), null);
                return;
            }
            var d = debtOpt.get();
            String msg = BotMessageBuilder.buildDebtDetailMessage(d);
            apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDebtActionKeyboard(d), "HTML");
        } catch (Exception e) {
            apiClient.sendMessage(chatId, "⚠️ Qarz topilmadi.", replyKeyboardFactory.getMainMenu(), null);
        }
    }
}
