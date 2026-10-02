package com.hisobchi.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bot")
public class BotConfig {

    private String token = "";
    private String username = "hisobchi_bot";
    private String defaultTimezone = "Asia/Tashkent";
    private SpeechToTextConfig speechToText = new SpeechToTextConfig();
    private AiConfig ai = new AiConfig();
    private WebhookConfig webhook = new WebhookConfig();
    private int draftTtlMinutes = 30;

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(defaultTimezone);
        } catch (Exception e) {
            return ZoneId.of("Asia/Tashkent");
        }
    }

    @Getter
    @Setter
    public static class SpeechToTextConfig {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "whisper-1";
    }

    @Getter
    @Setter
    public static class AiConfig {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class WebhookConfig {
        private boolean enabled = true;
        private String url = "https://hisobchi-bot-4g83.onrender.com/api/telegram/webhook";
        private String secretToken = "";
    }
}
