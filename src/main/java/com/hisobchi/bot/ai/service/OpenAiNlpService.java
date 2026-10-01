package com.hisobchi.bot.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobchi.bot.ai.dto.ParsedTransaction;
import com.hisobchi.bot.config.BotConfig;
import com.hisobchi.bot.transaction.entity.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiNlpService {

    private final BotConfig botConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Siz O'zbek tilidagi shaxsiy hisobchi Telegram bot uchun moliyaviy xabarlarni tahlil qiluvchi NLP modelsiz.
            Foydalanuvchi xabaridan quyidagi JSON formatida ma'lumotlarni ajratib bering:
            {
              "type": "EXPENSE" yoki "INCOME",
              "amount": 15000,
              "category": "Kategoriya nomi",
              "description": "Qisqa izoh",
              "confidence": 0.95
            }
            Kategoriyalar:
            Xarajat uchun: Ovqat, Transport, Yoqilg‘i, Material, Ish, Uy, Bozor, Kommunal, Aloqa / Internet, Oila, Sovg‘a, Dori, Ko‘ngilochar, Kredit / Qarzdorlik, Ta’lim, Sayohat, Boshqa.
            Daromad uchun: Ish, Xizmat, Savdo, Naqd, O‘tkazma, Boshqa.
            Faqat va faqat to'g'ri JSON qaytaring, ortiqcha so'z yoki markdown yozmang.
            """;

    public Optional<ParsedTransaction> parseWithAi(String text) {
        if (!botConfig.getAi().isEnabled()) {
            return Optional.empty();
        }

        String apiKey = botConfig.getAi().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String baseUrl = botConfig.getAi().getBaseUrl();
            String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "chat/completions";
            String model = botConfig.getAi().getModel();

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", text)
                    ),
                    "temperature", 0.1
            );

            String response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (response != null && !response.isBlank()) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && !choices.isEmpty()) {
                    String content = choices.get(0).get("message").get("content").asText();
                    JsonNode json = objectMapper.readTree(content);

                    String typeStr = json.path("type").asText("EXPENSE");
                    TransactionType type = "INCOME".equalsIgnoreCase(typeStr) ? TransactionType.INCOME : TransactionType.EXPENSE;
                    BigDecimal amount = new BigDecimal(json.path("amount").asText("0"));
                    String category = json.path("category").asText("Boshqa");
                    String description = json.path("description").asText("");
                    double confidence = json.path("confidence").asDouble(0.9);

                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        return Optional.of(new ParsedTransaction(type, amount, category, description, confidence, text));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI fallback parsing failed: {}", e.getMessage());
        }

        return Optional.empty();
    }
}
