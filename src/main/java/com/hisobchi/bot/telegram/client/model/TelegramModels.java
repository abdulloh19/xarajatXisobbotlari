package com.hisobchi.bot.telegram.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class TelegramModels {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Update {
        @JsonProperty("update_id")
        private Long updateId;

        @JsonProperty("message")
        private Message message;

        @JsonProperty("callback_query")
        private CallbackQuery callbackQuery;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        @JsonProperty("message_id")
        private Integer messageId;

        @JsonProperty("from")
        private User from;

        @JsonProperty("chat")
        private Chat chat;

        @JsonProperty("date")
        private Integer date;

        @JsonProperty("text")
        private String text;

        @JsonProperty("voice")
        private Voice voice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class User {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("is_bot")
        private Boolean isBot;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("username")
        private String username;

        @JsonProperty("language_code")
        private String languageCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Chat {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("username")
        private String username;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Voice {
        @JsonProperty("file_id")
        private String fileId;

        @JsonProperty("file_unique_id")
        private String fileUniqueId;

        @JsonProperty("duration")
        private Integer duration;

        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("file_size")
        private Long fileSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallbackQuery {
        @JsonProperty("id")
        private String id;

        @JsonProperty("from")
        private User from;

        @JsonProperty("message")
        private Message message;

        @JsonProperty("data")
        private String data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TelegramFile {
        @JsonProperty("file_id")
        private String fileId;

        @JsonProperty("file_unique_id")
        private String fileUniqueId;

        @JsonProperty("file_size")
        private Long fileSize;

        @JsonProperty("file_path")
        private String filePath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        @JsonProperty("ok")
        private Boolean ok;

        @JsonProperty("result")
        private T result;

        @JsonProperty("description")
        private String description;

        @JsonProperty("error_code")
        private Integer errorCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InlineKeyboardMarkup {
        @JsonProperty("inline_keyboard")
        private List<List<InlineKeyboardButton>> inlineKeyboard;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InlineKeyboardButton {
        @JsonProperty("text")
        private String text;

        @JsonProperty("callback_data")
        private String callbackData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplyKeyboardMarkup {
        @JsonProperty("keyboard")
        private List<List<KeyboardButton>> keyboard;

        @JsonProperty("resize_keyboard")
        @Builder.Default
        private Boolean resizeKeyboard = true;

        @JsonProperty("one_time_keyboard")
        @Builder.Default
        private Boolean oneTimeKeyboard = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeyboardButton {
        @JsonProperty("text")
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplyKeyboardRemove {
        @JsonProperty("remove_keyboard")
        @Builder.Default
        private Boolean removeKeyboard = true;
    }
}
