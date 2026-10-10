package com.hisobchi.bot.todo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobchi.bot.config.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoAiAssistantService {

    private final BotConfig botConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String DECOMPOSITION_PROMPT = """
            Siz O'zbek tilida professional unumdorlik (productivity) va vazifalar boshqaruvchisi modelsiz.
            Foydalanuvchi taqdim etgan vazifani 3 tadan 5 tagacha mantiqiy, ketma-ket, aniq va bajarilishi oson kichik qadamlarga (subtask) bo'lib bering.
            Faqat quyidagi JSON formatida qaytaring, boshqa hech qanday so'z yoki markdown yozmang:
            {
              "subtasks": [
                "1-qadam...",
                "2-qadam...",
                "3-qadam..."
              ]
            }
            """;

    public List<String> suggestSubtasks(String taskTitle) {
        if (botConfig.getAi().isEnabled() && botConfig.getAi().getApiKey() != null && !botConfig.getAi().getApiKey().isBlank()) {
            try {
                String baseUrl = botConfig.getAi().getBaseUrl();
                String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "chat/completions";
                String model = botConfig.getAi().getModel();

                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "response_format", Map.of("type", "json_object"),
                        "messages", List.of(
                                Map.of("role", "system", "content", DECOMPOSITION_PROMPT),
                                Map.of("role", "user", "content", "Vazifa: " + taskTitle)
                        ),
                        "temperature", 0.3
                );

                String response = restClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + botConfig.getAi().getApiKey())
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
                        JsonNode arr = json.get("subtasks");
                        if (arr != null && arr.isArray()) {
                            List<String> list = new ArrayList<>();
                            for (JsonNode node : arr) {
                                String s = node.asText().trim();
                                if (!s.isBlank()) list.add(s);
                            }
                            if (!list.isEmpty()) return list;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("AI subtask decomposition call failed: {}. Using heuristic fallback.", e.getMessage());
            }
        }

        // Heuristic fallback
        return generateHeuristicSubtasks(taskTitle);
    }

    private List<String> generateHeuristicSubtasks(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("ta'mir") || lower.contains("remont") || lower.contains("qurilish")) {
            return List.of(
                    "Reja va smetani ko‘rib chiqish",
                    "Zarur materiallar ro‘yxatini tuzish",
                    "Do‘kondan mahsulotlarni xarid qilish",
                    "Usta bilan ishni boshlash",
                    "Bajarilgan ishni qabul qilib, hisob-kitob qilish"
            );
        } else if (lower.contains("hisobot") || lower.contains("report") || lower.contains("hujjat")) {
            return List.of(
                    "Kerakli raqamlar va ma'lumotlarni yig‘ish",
                    "Qoralama hisobotni tayyorlash",
                    "Hisobotni tekshirib chiqish",
                    "Tayyor hisobotni topshirish"
            );
        } else if (lower.contains("to‘y") || lower.contains("ziyofat") || lower.contains("tadbir")) {
            return List.of(
                    "Mehmonlar ro‘yxatini shakllantirish",
                    "Joy va taomnomani belgilash",
                    "Xarajatlar byudjetini hisoblash",
                    "Yakuniy tayyorgarlikni ko‘rish"
            );
        } else {
            return List.of(
                    "Boshlang‘ich tayyorgarlik ko‘rish",
                    "Asosiy jarayonni bajarish",
                    "Natijani tekshirish va yakunlash"
            );
        }
    }
}
