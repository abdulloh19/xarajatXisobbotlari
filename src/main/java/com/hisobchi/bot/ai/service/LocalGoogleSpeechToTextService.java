package com.hisobchi.bot.ai.service;

import com.hisobchi.bot.ai.dto.TranscriptionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LocalGoogleSpeechToTextService implements SpeechToTextService {

    private static final String SCRIPT_PATH = "scripts/transcribe_helper.py";

    @Override
    public TranscriptionResult transcribe(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            return TranscriptionResult.failure("Audio fayl topilmadi");
        }

        File scriptFile = new File(SCRIPT_PATH);
        if (!scriptFile.exists()) {
            log.warn("Transcribe helper script not found at {}", scriptFile.getAbsolutePath());
            return TranscriptionResult.failure("Ovozni aniqlash skripti topilmadi");
        }

        try {
            log.info("Running local/Google speech-to-text recognition for {}", audioFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder("python", scriptFile.getAbsolutePath(), audioFile.getAbsolutePath());
            pb.redirectErrorStream(false);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(" ");
                }
            }

            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String errLine;
                while ((errLine = errReader.readLine()) != null) {
                    errorOutput.append(errLine).append(" ");
                }
            }

            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return TranscriptionResult.failure("Ovozni aniqlash vaqti tugadi");
            }

            int exitCode = process.exitValue();
            String resultText = output.toString().trim();

            if (exitCode == 0 && !resultText.isBlank()) {
                log.info("Local/Google Speech recognition succeeded: '{}'", resultText);
                return TranscriptionResult.success(resultText);
            } else {
                log.warn("Speech recognition exited with code {}: {}", exitCode, errorOutput);
                String err = errorOutput.toString().trim();
                if (err.contains("Ovozdan so'zlar aniqlanmadi")) {
                    return TranscriptionResult.failure("Ovozdan so'zlar aniqlanmadi. Iltimos, aniqroq va balandroq gapiring.");
                }
                return TranscriptionResult.failure("Ovozni aniqlab bo‘lmadi: " + (err.isBlank() ? "xatolik" : err));
            }

        } catch (Exception e) {
            log.error("Error executing speech recognition process: {}", e.getMessage(), e);
            return TranscriptionResult.failure("Ovozni matnga aylantirishda tizim xatosi: " + e.getMessage());
        }
    }
}
