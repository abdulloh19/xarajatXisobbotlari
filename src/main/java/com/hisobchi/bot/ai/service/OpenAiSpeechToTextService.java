package com.hisobchi.bot.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobchi.bot.ai.dto.TranscriptionResult;
import com.hisobchi.bot.config.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiSpeechToTextService implements SpeechToTextService {

    private final BotConfig botConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public TranscriptionResult transcribe(File audioFile) {
        String apiKey = botConfig.getSpeechToText().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Speech-to-text API key is not configured");
            return TranscriptionResult.failure("Ovozni aniqlash xizmati kaliti (AI_API_KEY) sozlanmagan");
        }

        if (audioFile == null || !audioFile.exists()) {
            return TranscriptionResult.failure("Audio fayl topilmadi");
        }

        try {
            String baseUrl = botConfig.getSpeechToText().getBaseUrl();
            String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "audio/transcriptions";
            String model = botConfig.getSpeechToText().getModel();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(audioFile));
            body.add("model", model);
            body.add("language", "uz");

            log.info("Sending audio to speech-to-text service: {}, file size: {} bytes",
                    url, audioFile.length());

            String response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (response != null && !response.isBlank()) {
                JsonNode root = objectMapper.readTree(response);
                if (root.has("text")) {
                    String transcribed = root.get("text").asText().trim();
                    log.info("Speech transcription successful: '{}'", transcribed);
                    return TranscriptionResult.success(transcribed);
                }
            }

            return TranscriptionResult.failure("Ovozdan matn olinmadi");
        } catch (Exception e) {
            log.error("Speech transcription error: {}", e.getMessage(), e);
            return TranscriptionResult.failure("Ovozni matnga aylantirishda xatolik: " + e.getMessage());
        }
    }
}
