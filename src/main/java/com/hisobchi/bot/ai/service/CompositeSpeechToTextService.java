package com.hisobchi.bot.ai.service;

import com.hisobchi.bot.ai.dto.TranscriptionResult;
import com.hisobchi.bot.config.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class CompositeSpeechToTextService implements SpeechToTextService {

    private final BotConfig botConfig;
    private final OpenAiSpeechToTextService openAiSpeechToTextService;
    private final LocalGoogleSpeechToTextService localGoogleSpeechToTextService;

    @Override
    public TranscriptionResult transcribe(File audioFile) {
        String apiKey = botConfig.getSpeechToText().getApiKey();

        // 1. If OpenAI API key is configured, try OpenAI Whisper
        if (apiKey != null && !apiKey.isBlank() && apiKey.startsWith("sk-")) {
            log.info("Attempting speech-to-text with OpenAI Whisper...");
            TranscriptionResult result = openAiSpeechToTextService.transcribe(audioFile);
            if (result.success() && !result.text().isBlank()) {
                return result;
            }
            log.warn("OpenAI Whisper failed ({}), falling back to built-in Google Speech Recognition...",
                    result.errorMessage());
        }

        // 2. Built-in free Speech Recognition (Google Web Speech API in Uzbek)
        log.info("Using built-in Speech Recognition for audio...");
        return localGoogleSpeechToTextService.transcribe(audioFile);
    }
}
