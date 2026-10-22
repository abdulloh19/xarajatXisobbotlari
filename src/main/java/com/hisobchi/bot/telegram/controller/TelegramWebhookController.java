package com.hisobchi.bot.telegram.controller;

import com.hisobchi.bot.config.BotConfig;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.Update;
import com.hisobchi.bot.telegram.handler.UpdateDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final UpdateDispatcher updateDispatcher;
    private final BotConfig botConfig;
    private final TelegramApiClient apiClient;
    private final com.hisobchi.bot.user.repository.UserRepository userRepository;
    private final com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory replyKeyboardFactory;
    private final ExecutorService updateExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @RequestMapping(value = "/init-menu", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> initMenu() {
        apiClient.initBotMenuAndCommands();
        return ResponseEntity.ok("Telegram menu and commands initialized successfully!");
    }

    @RequestMapping(value = "/refresh-user-menu", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> refreshUserMenu() {
        apiClient.initBotMenuAndCommands();
        var users = userRepository.findAll();
        int count = 0;
        for (var u : users) {
            if (u.getTelegramId() != null) {
                try {
                    apiClient.sendMessage(u.getTelegramId(),
                            "✨ <b>Asosiy menyu yangilandi!</b>\n\nPastdagi menyuda faqat asosiy bo‘limlar qoldirildi:\n" +
                            "📊 Kunlik natija | 🗓 Oylik natija\n" +
                            "🤝 Qarzlar | 🗑 Tarixni tozalash",
                            replyKeyboardFactory.getMainMenu(), "HTML");
                    count++;
                } catch (Exception e) {
                    log.warn("Could not send menu to user {}: {}", u.getTelegramId(), e.getMessage());
                }
            }
        }
        return ResponseEntity.ok("Updated menu sent to " + count + " users!");
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody(required = false) Update update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken) {

        String expectedSecret = botConfig.getWebhook() != null ? botConfig.getWebhook().getSecretToken() : null;
        if (expectedSecret != null && !expectedSecret.isBlank()) {
            if (!expectedSecret.equals(secretToken)) {
                log.warn("Invalid webhook secret token received!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        if (update != null) {
            updateExecutor.submit(() -> {
                try {
                    updateDispatcher.dispatch(update);
                } catch (Exception e) {
                    log.error("Error processing webhook update: {}", e.getMessage(), e);
                }
            });
        }

        return ResponseEntity.ok().build();
    }
}
