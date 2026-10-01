package com.hisobchi.bot.voice.service;

import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.TelegramFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioFileDownloader {

    private final TelegramApiClient telegramApiClient;

    public File downloadVoice(String fileId) throws IOException {
        TelegramFile fileInfo = telegramApiClient.getFile(fileId);
        if (fileInfo == null || fileInfo.getFilePath() == null) {
            throw new IOException("Telegramdan fayl ma'lumoti olinmadi: " + fileId);
        }

        File tempFile = Files.createTempFile("voice_", ".ogg").toFile();
        boolean downloaded = telegramApiClient.downloadFile(fileInfo.getFilePath(), tempFile);

        if (!downloaded || !tempFile.exists() || tempFile.length() == 0) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new IOException("Ovozli xabarni yuklab olish muvaffaqiyatsiz bo‘ldi");
        }

        log.info("Downloaded Telegram voice file {} ({} bytes) to {}",
                fileId, tempFile.length(), tempFile.getAbsolutePath());
        return tempFile;
    }
}
