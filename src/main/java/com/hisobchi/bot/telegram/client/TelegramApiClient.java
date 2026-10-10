package com.hisobchi.bot.telegram.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobchi.bot.config.BotConfig;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramApiClient {

    private final BotConfig botConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";
    private static final String TELEGRAM_FILE_BASE = "https://api.telegram.org/file/bot";

    private String getApiUrl(String method) {
        return TELEGRAM_API_BASE + botConfig.getToken() + "/" + method;
    }

    private String getFileUrl(String filePath) {
        return TELEGRAM_FILE_BASE + botConfig.getToken() + "/" + filePath;
    }

    private String executePost(String method, Object body) {
        byte[] bytes = restClient.post()
                .uri(getApiUrl(method))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(byte[].class);
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : "";
    }

    public Message sendMessage(Long chatId, String text, Object replyMarkup, String parseMode) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) {
            log.warn("Telegram bot token is not configured. Simulating sendMessage to {}: {}", chatId, text);
            return Message.builder().chat(Chat.builder().id(chatId).build()).text(text).build();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            if (parseMode != null && !parseMode.isBlank()) {
                body.put("parse_mode", parseMode);
            }
            if (replyMarkup != null) {
                body.put("reply_markup", replyMarkup);
            }

            String jsonResponse = executePost("sendMessage", body);
            ApiResponse<Message> apiResponse = objectMapper.readValue(
                    jsonResponse, new TypeReference<ApiResponse<Message>>() {});
            return apiResponse.getResult();
        } catch (Exception e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
            return null;
        }
    }

    public void editMessageText(Long chatId, Integer messageId, String text, InlineKeyboardMarkup replyMarkup, String parseMode) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) {
            log.debug("Simulating editMessageText for chat {} message {}: {}", chatId, messageId, text);
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("message_id", messageId);
            body.put("text", text);
            if (parseMode != null && !parseMode.isBlank()) {
                body.put("parse_mode", parseMode);
            }
            if (replyMarkup != null) {
                body.put("reply_markup", replyMarkup);
            }

            executePost("editMessageText", body);
        } catch (Exception e) {
            log.error("Failed to edit message in chat {}: {}", chatId, e.getMessage());
        }
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) return;

        try {
            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "message_id", messageId
            );
            executePost("deleteMessage", body);
        } catch (Exception e) {
            log.warn("Failed to delete message {} in chat {}: {}", messageId, chatId, e.getMessage());
        }
    }

    public void answerCallbackQuery(String callbackQueryId, String text, Boolean showAlert) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) return;

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("callback_query_id", callbackQueryId);
            if (text != null && !text.isBlank()) {
                body.put("text", text);
            }
            if (showAlert != null) {
                body.put("show_alert", showAlert);
            }

            executePost("answerCallbackQuery", body);
        } catch (Exception e) {
            log.warn("Failed to answer callback query {}: {}", callbackQueryId, e.getMessage());
        }
    }

    public TelegramFile getFile(String fileId) {
        try {
            String jsonResponse = executePost("getFile", Map.of("file_id", fileId));
            ApiResponse<TelegramFile> response = objectMapper.readValue(
                    jsonResponse, new TypeReference<ApiResponse<TelegramFile>>() {});
            return response.getResult();
        } catch (Exception e) {
            log.error("Failed to get file info for {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    public boolean downloadFile(String filePath, File destination) {
        try {
            String url = getFileUrl(filePath);
            ResponseEntity<Resource> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(Resource.class);

            if (response.getBody() != null) {
                try (InputStream in = response.getBody().getInputStream();
                     FileOutputStream out = new FileOutputStream(destination)) {
                    in.transferTo(out);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to download file from {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    public List<Update> getUpdates(Long offset, Integer timeout, Integer limit) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) {
            return List.of();
        }

        try {
            Map<String, Object> params = new HashMap<>();
            if (offset != null) params.put("offset", offset);
            if (timeout != null) params.put("timeout", timeout);
            if (limit != null) params.put("limit", limit);

            String jsonResponse = executePost("getUpdates", params);
            if (jsonResponse == null || jsonResponse.isBlank()) {
                return List.of();
            }

            ApiResponse<List<Update>> response = objectMapper.readValue(
                    jsonResponse, new TypeReference<ApiResponse<List<Update>>>() {});
            return response.getResult() != null ? response.getResult() : List.of();
        } catch (Exception e) {
            log.debug("Long-polling getUpdates error (will retry): {}", e.getMessage());
            return List.of();
        }
    }

    public boolean setWebhook(String url, String secretToken) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) return false;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("url", url);
            if (secretToken != null && !secretToken.isBlank()) {
                body.put("secret_token", secretToken);
            }
            body.put("allowed_updates", List.of("message", "callback_query"));
            String json = executePost("setWebhook", body);
            log.info("Telegram setWebhook to {}: {}", url, json);
            return true;
        } catch (Exception e) {
            log.error("Failed to set Telegram webhook to {}: {}", url, e.getMessage());
            return false;
        }
    }

    public boolean deleteWebhook() {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) return false;
        try {
            String json = executePost("deleteWebhook", Map.of("drop_pending_updates", false));
            log.info("Telegram deleteWebhook: {}", json);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete Telegram webhook: {}", e.getMessage());
            return false;
        }
    }

    public boolean setChatMenuButtonDefault() {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) return false;
        try {
            Map<String, Object> body = Map.of(
                    "menu_button", Map.of("type", "commands")
            );
            String json = executePost("setChatMenuButton", body);
            log.info("Telegram setChatMenuButton: {}", json);
            return true;
        } catch (Exception e) {
            log.warn("Failed to reset Telegram chat menu button: {}", e.getMessage());
            return false;
        }
    }

    public boolean setMyCommands(List<Map<String, String>> commands) {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) return false;
        try {
            Map<String, Object> body = Map.of("commands", commands);
            String json = executePost("setMyCommands", body);
            log.info("Telegram setMyCommands: {}", json);
            return true;
        } catch (Exception e) {
            log.warn("Failed to set Telegram commands: {}", e.getMessage());
            return false;
        }
    }

    public void initBotMenuAndCommands() {
        if (botConfig.getToken() == null || botConfig.getToken().isBlank()) return;
        try {
            setChatMenuButtonDefault();
            setMyCommands(List.of(
                    Map.of("command", "start", "description", "🏠 Asosiy menyu"),
                    Map.of("command", "bugun", "description", "📅 Bugungi kun sharhi"),
                    Map.of("command", "vazifalar", "description", "✅ Vazifalar va eslatmalar"),
                    Map.of("command", "xarajat", "description", "💸 Xarajat qo‘shish"),
                    Map.of("command", "daromad", "description", "💰 Daromad qo‘shish"),
                    Map.of("command", "foyda", "description", "💵 Kunlik foydani kiritish"),
                    Map.of("command", "statistika", "description", "📊 Bugungi statistika"),
                    Map.of("command", "hisobot", "description", "📊 Davriy hisobotlar"),
                    Map.of("command", "qarzlar", "description", "🤝 Qarzlar daftari"),
                    Map.of("command", "tarix", "description", "📜 Operatsiyalar tarixi"),
                    Map.of("command", "kategoriya", "description", "📂 Yangi kategoriya qo‘shish"),
                    Map.of("command", "kunni_yopish", "description", "🔐 Kunni yopish"),
                    Map.of("command", "sozlamalar", "description", "⚙️ Sozlamalar"),
                    Map.of("command", "yordam", "description", "ℹ️ Yordam va qo‘llanma")
            ));
        } catch (Exception e) {
            log.warn("Could not initialize Telegram bot menu and commands: {}", e.getMessage());
        }
    }
}
