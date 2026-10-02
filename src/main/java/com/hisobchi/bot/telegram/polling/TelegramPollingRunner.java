package com.hisobchi.bot.telegram.polling;

import com.hisobchi.bot.config.BotConfig;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.Update;
import com.hisobchi.bot.telegram.handler.UpdateDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramPollingRunner implements CommandLineRunner, AutoCloseable {

    private final BotConfig botConfig;
    private final TelegramApiClient apiClient;
    private final UpdateDispatcher updateDispatcher;

    private volatile boolean running = true;
    private Long lastUpdateId = 0L;

    @Override
    public void run(String... args) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) {
            log.info("Telegram Bot Token is not set. Bot is running in offline/API mode.");
            return;
        }

        if (botConfig.getWebhook() != null && botConfig.getWebhook().isEnabled()) {
            String webhookUrl = botConfig.getWebhook().getUrl();
            log.info("Telegram Webhook mode enabled. Setting webhook to: {}", webhookUrl);
            boolean success = apiClient.setWebhook(webhookUrl, botConfig.getWebhook().getSecretToken());
            if (success) {
                log.info("Telegram webhook successfully registered to {}. Long-polling runner skipped.", webhookUrl);
                return;
            } else {
                log.warn("Failed to register Telegram webhook. Falling back to long-polling mode.");
            }
        } else {
            log.info("Telegram Webhook is disabled. Clearing webhook for long-polling...");
            apiClient.deleteWebhook();
        }

        Thread pollingThread = new Thread(this::pollUpdates, "telegram-polling-worker");
        pollingThread.setDaemon(true);
        pollingThread.start();
        log.info("Started Telegram long-polling thread for @{}", botConfig.getUsername());
    }

    private void pollUpdates() {
        while (running) {
            try {
                Long offset = (lastUpdateId > 0) ? lastUpdateId + 1 : null;
                List<Update> updates = apiClient.getUpdates(offset, 25, 50);

                for (Update update : updates) {
                    if (update.getUpdateId() != null) {
                        lastUpdateId = Math.max(lastUpdateId, update.getUpdateId());
                    }
                    updateDispatcher.dispatch(update);
                }

                if (updates.isEmpty()) {
                    Thread.sleep(500);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Error during polling updates: {}. Retrying in 3 seconds...", e.getMessage());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override
    public void close() {
        this.running = false;
        log.info("Stopped Telegram long-polling thread.");
    }
}
