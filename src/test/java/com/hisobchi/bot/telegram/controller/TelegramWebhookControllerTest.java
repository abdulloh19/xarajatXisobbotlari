package com.hisobchi.bot.telegram.controller;

import com.hisobchi.bot.config.BotConfig;
import com.hisobchi.bot.telegram.client.model.TelegramModels.Update;
import com.hisobchi.bot.telegram.handler.UpdateDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {

    @Mock
    private UpdateDispatcher updateDispatcher;

    @Mock
    private BotConfig botConfig;

    @InjectMocks
    private TelegramWebhookController webhookController;

    private BotConfig.WebhookConfig webhookConfig;

    @BeforeEach
    void setUp() {
        webhookConfig = new BotConfig.WebhookConfig();
        lenient().when(botConfig.getWebhook()).thenReturn(webhookConfig);
    }

    @Test
    @DisplayName("Valid webhook update should return 200 OK")
    void testValidWebhookUpdate() {
        Update update = new Update();
        update.setUpdateId(12345L);

        ResponseEntity<Void> response = webhookController.handleWebhook(update, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Invalid secret token should return 403 Forbidden")
    void testInvalidSecretToken() {
        webhookConfig.setSecretToken("my_secret_token_123");

        Update update = new Update();
        update.setUpdateId(12345L);

        ResponseEntity<Void> response = webhookController.handleWebhook(update, "wrong_token");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Valid secret token should return 200 OK")
    void testValidSecretToken() {
        webhookConfig.setSecretToken("my_secret_token_123");

        Update update = new Update();
        update.setUpdateId(12345L);

        ResponseEntity<Void> response = webhookController.handleWebhook(update, "my_secret_token_123");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
