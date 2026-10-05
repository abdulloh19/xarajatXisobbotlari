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
                                new KeyboardButton("💸 Xarajat qo‘shish"),
                                new KeyboardButton("💰 Daromad qo‘shish")
                        ),
                        List.of(
                                new KeyboardButton("🎙 Ovoz bilan kiritish")
                        ),
                        List.of(
                                new KeyboardButton("📊 Bugungi statistika"),
                                new KeyboardButton("📅 Haftalik statistika")
                        ),
                        List.of(
                                new KeyboardButton("🗓 Oylik statistika"),
                                new KeyboardButton("📜 Tarix")
                        ),
                        List.of(
                                new KeyboardButton("💵 Foydani kiritish"),
                                new KeyboardButton("🤝 Qarzlar")
                        ),
                        List.of(
                                new KeyboardButton("📊 Hisobotlar"),
                                new KeyboardButton("📂 Kategoriyalar")
                        ),
                        List.of(
                                new KeyboardButton("⚙️ Sozlamalar"),
                                new KeyboardButton("🔐 Kunni yopish")
                        )
                ))
                .build();
    }

    public ReplyKeyboardMarkup getReportsMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(
                                new KeyboardButton("📅 Bugun"),
                                new KeyboardButton("📆 Oxirgi 7 kun")
                        ),
                        List.of(
                                new KeyboardButton("📆 Oxirgi 14 kun"),
                                new KeyboardButton("📆 Oxirgi 21 kun")
                        ),
                        List.of(
                                new KeyboardButton("🗓 Shu oy"),
                                new KeyboardButton("🗓 O‘tgan oy")
                        ),
                        List.of(
                                new KeyboardButton("⬅️ Asosiy menyu")
                        )
                ))
                .build();
    }

    public ReplyKeyboardMarkup getDebtsMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(
                                new KeyboardButton("💰 Qarz oldim"),
                                new KeyboardButton("💸 Qarz berdim")
                        ),
                        List.of(
                                new KeyboardButton("💳 Qarz to‘lash"),
                                new KeyboardButton("💵 Qarz qaytardi")
                        ),
                        List.of(
                                new KeyboardButton("📋 Men olgan qarzlar"),
                                new KeyboardButton("📋 Men bergan qarzlar")
                        ),
                        List.of(
                                new KeyboardButton("⚠️ Muddati yaqin"),
                                new KeyboardButton("✅ Yopilgan qarzlar")
                        ),
                        List.of(
                                new KeyboardButton("📊 Qarz statistikasi"),
                                new KeyboardButton("⬅️ Asosiy menyu")
                        )
                ))
                .build();
    }

    public ReplyKeyboardMarkup getPaymentMethodMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(
                                new KeyboardButton("💵 Naqd"),
                                new KeyboardButton("💳 Karta")
                        ),
                        List.of(
                                new KeyboardButton("❌ Bekor qilish")
                        )
                ))
                .build();
    }

    public ReplyKeyboardMarkup getCancelMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(new KeyboardButton("❌ Bekor qilish"))
                ))
                .build();
    }

    public ReplyKeyboardMarkup getSkipOrCancelMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(new KeyboardButton("⏭ O‘tkazib yuborish")),
                        List.of(new KeyboardButton("❌ Bekor qilish"))
                ))
                .build();
    }

    public ReplyKeyboardMarkup getHistoryMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(
                                new KeyboardButton("📅 Bugun"),
                                new KeyboardButton("📅 Kecha")
                        ),
                        List.of(
                                new KeyboardButton("📅 Oxirgi 7 kun"),
                                new KeyboardButton("🗓 Shu oy")
                        ),
                        List.of(
                                new KeyboardButton("⬅️ Asosiy menyu")
                        )
                ))
                .build();
    }

    public ReplyKeyboardMarkup getSettingsMenu() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        List.of(
                                new KeyboardButton("➕ Yangi kategoriya"),
                                new KeyboardButton("🔔 Eslatmalar")
                        ),
                        List.of(
                                new KeyboardButton("🔄 Ma'lumotlarni tozalash")
                        ),
                        List.of(new KeyboardButton("⬅️ Asosiy menyu"))
                ))
                .build();
    }
}
