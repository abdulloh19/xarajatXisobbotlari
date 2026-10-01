package com.hisobchi.bot.voice.service;

import com.hisobchi.bot.ai.dto.ParsedTransaction;
import com.hisobchi.bot.ai.dto.TranscriptionResult;
import com.hisobchi.bot.ai.service.SpeechToTextService;
import com.hisobchi.bot.ai.service.TransactionNlpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceProcessingService {

    private final AudioFileDownloader audioFileDownloader;
    private final SpeechToTextService speechToTextService;
    private final TransactionNlpService nlpService;

    public record VoiceProcessResult(
            TranscriptionResult transcription,
            Optional<ParsedTransaction> parsedTransaction
    ) {}

    public VoiceProcessResult processVoice(String fileId) {
        File tempAudio = null;
        try {
            tempAudio = audioFileDownloader.downloadVoice(fileId);
            TranscriptionResult transcription = speechToTextService.transcribe(tempAudio);

            if (!transcription.success() || transcription.text().isBlank()) {
                log.warn("Speech transcription failed or empty: {}", transcription.errorMessage());
                return new VoiceProcessResult(transcription, Optional.empty());
            }

            Optional<ParsedTransaction> parsed = nlpService.parse(transcription.text());
            return new VoiceProcessResult(transcription, parsed);

        } catch (Exception e) {
            log.error("Failed to process voice message {}: {}", fileId, e.getMessage(), e);
            return new VoiceProcessResult(
                    TranscriptionResult.failure("Ovozli xabarni qayta ishlashda xatolik: " + e.getMessage()),
                    Optional.empty()
            );
        } finally {
            // Strictly delete temporary audio file as required by specification
            if (tempAudio != null && tempAudio.exists()) {
                boolean deleted = tempAudio.delete();
                if (deleted) {
                    log.debug("Temporary audio file deleted: {}", tempAudio.getAbsolutePath());
                } else {
                    log.warn("Failed to delete temp audio file: {}", tempAudio.getAbsolutePath());
                }
            }
        }
    }
}
