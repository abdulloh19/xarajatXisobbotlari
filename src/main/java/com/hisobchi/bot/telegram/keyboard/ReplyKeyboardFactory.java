package com.hisobchi.bot.telegram.keyboard;

import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReplyKeyboardFactory {

    public ReplyKeyboardMarkup getMainMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(
                                new KeyboardButton("📊 Kunlik natija"),
                                new KeyboardButton("🗓 Oylik natija")
                        ),
                        List.of(
                                new KeyboardButton("🤝 Qarzlar"),
                                new KeyboardButton("🗑 Tarixni tozalash")
                        )
                ))
                .build();
    }

    public ReplyKeyboardMarkup getReportsMenu() {
        return getMainMenu();
    }

    public ReplyKeyboardMarkup getDebtsMenu() {
        return getMainMenu();
    }

    public ReplyKeyboardMarkup getPaymentMethodMenu() {
        return getMainMenu();
    }

    public ReplyKeyboardMarkup getCancelMenu() {
        return getMainMenu();
    }

    public ReplyKeyboardMarkup getSkipOrCancelMenu() {
        return getMainMenu();
    }

    public ReplyKeyboardMarkup getHistoryMenu() {
        return getMainMenu();
    }

    public ReplyKeyboardMarkup getSettingsMenu() {
        return getMainMenu();
    }
}
