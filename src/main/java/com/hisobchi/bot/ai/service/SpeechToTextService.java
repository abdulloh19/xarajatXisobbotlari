package com.hisobchi.bot.ai.service;

import com.hisobchi.bot.ai.dto.TranscriptionResult;

import java.io.File;

public interface SpeechToTextService {
    TranscriptionResult transcribe(File audioFile);
}
