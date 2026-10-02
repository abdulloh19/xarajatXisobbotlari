package com.hisobchi.bot.telegram.controller;

import com.hisobchi.bot.config.BotConfig;
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
    private final ExecutorService updateExecutor = Executors.newVirtualThreadPerTaskExecutor();

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
