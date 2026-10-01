package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.idempotency.service.IdempotencyService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDispatcher {

    private final UserService userService;
    private final CommandHandler commandHandler;
    private final TextMessageHandler textMessageHandler;
    private final VoiceMessageHandler voiceMessageHandler;
    private final CallbackQueryHandler callbackQueryHandler;
    private final TelegramApiClient apiClient;
    private final IdempotencyService idempotencyService;

    public void dispatch(Update update) {
        if (update == null) return;

        // Idempotency check: discard updates that have already been processed
        if (update.getUpdateId() != null) {
            if (idempotencyService.isUpdateProcessed(update.getUpdateId())) {
                log.debug("Skipping already processed update: {}", update.getUpdateId());
                return;
            }
            idempotencyService.markUpdateProcessed(update.getUpdateId());
        }

        try {
            if (update.getMessage() != null) {
                handleMessageUpdate(update.getMessage());
            } else if (update.getCallbackQuery() != null) {
                handleCallbackQueryUpdate(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Unhandled error processing update {}: {}", update.getUpdateId(), e.getMessage(), e);
            Long chatId = extractChatId(update);
            if (chatId != null) {
                apiClient.sendMessage(chatId, "⚠️ Xatolik yuz berdi. Qayta urinib ko‘ring.", null, null);
            }
        }
    }

    private void handleMessageUpdate(Message message) {
        if (message.getFrom() == null || message.getChat() == null) return;

        com.hisobchi.bot.telegram.client.model.TelegramModels.User from = message.getFrom();
        User user = userService.getOrCreateUser(
                from.getId(), from.getFirstName(), from.getLastName(), from.getUsername());

        Long chatId = message.getChat().getId();

        if (message.getText() != null) {
            String text = message.getText().trim();
            if (text.startsWith("/")) {
                handleCommand(user, chatId, text);
            } else {
                textMessageHandler.handle(user, message);
            }
        } else if (message.getVoice() != null) {
            voiceMessageHandler.handle(user, message);
        }
    }

    private void handleCallbackQueryUpdate(CallbackQuery callbackQuery) {
        if (callbackQuery.getFrom() == null || callbackQuery.getMessage() == null) return;

        com.hisobchi.bot.telegram.client.model.TelegramModels.User from = callbackQuery.getFrom();
        User user = userService.getOrCreateUser(
                from.getId(), from.getFirstName(), from.getLastName(), from.getUsername());

        callbackQueryHandler.handle(user, callbackQuery);
    }

    private void handleCommand(User user, Long chatId, String command) {
        if (command.startsWith("/start")) {
            commandHandler.handleStart(user, chatId);
        } else if (command.startsWith("/help")) {
            commandHandler.handleHelp(user, chatId);
        } else if (command.startsWith("/tx_")) {
            commandHandler.handleTransactionDetailCommand(user, chatId, command);
        } else if (command.startsWith("/debt_")) {
            commandHandler.handleDebtDetailCommand(user, chatId, command);
        } else {
            commandHandler.handleHelp(user, chatId);
        }
    }

    private Long extractChatId(Update update) {
        if (update.getMessage() != null && update.getMessage().getChat() != null) {
            return update.getMessage().getChat().getId();
        }
        if (update.getCallbackQuery() != null && update.getCallbackQuery().getMessage() != null &&
            update.getCallbackQuery().getMessage().getChat() != null) {
            return update.getCallbackQuery().getMessage().getChat().getId();
        }
        return null;
    }
}
