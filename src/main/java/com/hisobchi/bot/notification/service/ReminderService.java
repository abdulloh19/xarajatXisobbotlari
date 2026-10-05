package com.hisobchi.bot.notification.service;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.entity.ReminderDeliveryLog;
import com.hisobchi.bot.notification.entity.ReminderType;
import com.hisobchi.bot.notification.repository.ReminderDeliveryLogRepository;
import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardButton;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardMarkup;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final UserRepository userRepository;
    private final NotificationSettingsService settingsService;
    private final ReminderDeliveryLogRepository deliveryLogRepository;
    private final DailyProfitService dailyProfitService;
    private final TelegramApiClient apiClient;

    @Transactional
    public void processRemindersForCurrentMinute() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                processUserReminders(user);
            } catch (Exception e) {
                log.error("Failed to process reminder for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
    }

    private void processUserReminders(User user) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalTime time = now.toLocalTime();
        LocalDate today = now.toLocalDate();

        NotificationSettings settings = settingsService.getOrCreateSettings(user);

        // 1. 18:00 Reminder
        if (Boolean.TRUE.equals(settings.getProfitReminder18Enabled()) && time.getHour() == 18 && time.getMinute() == 0) {
            sendReminder18IfEligible(user, today);
        }

        // 2. 21:00 Reminder
        if (Boolean.TRUE.equals(settings.getProfitReminder21Enabled()) && time.getHour() == 21 && time.getMinute() == 0) {
            sendReminder21IfEligible(user, today);
        }
    }

    private void sendReminder18IfEligible(User user, LocalDate today) {
        if (deliveryLogRepository.existsByUserIdAndReminderTypeAndReminderDate(user.getId(), ReminderType.PROFIT_18, today)) {
            return;
        }

        Optional<DailyProfit> profitOpt = dailyProfitService.getProfit(user.getId(), today);
        String text;
        InlineKeyboardMarkup keyboard;

        if (profitOpt.isPresent() && profitOpt.get().getTotalProfit().compareTo(java.math.BigDecimal.ZERO) > 0) {
            text = String.format(
                    "🚕 <b>Bugungi hisobni yakunlash vaqti yaqinlashdi.</b>\n\n" +
                    "Bugungi kiritilgan foydangiz: <b>%s</b>\n\n" +
                    "Qiymatni yangilamoqchimisiz?",
                    MoneyFormatter.format(profitOpt.get().getTotalProfit())
            );
            keyboard = InlineKeyboardMarkup.builder()
                    .inlineKeyboard(List.of(
                            List.of(InlineKeyboardButton.builder().text("✅ Yangilash").callbackData("profit:enter").build()),
                            List.of(InlineKeyboardButton.builder().text("⏰ Keyinroq").callbackData("reminder:dismiss").build())
                    ))
                    .build();
        } else {
            text = "🚕 <b>Bugungi hisobni yakunlash vaqti yaqinlashdi.</b>\n\n" +
                   "✅ <b>Hozir qo‘lingizda qancha foyda qoldi?</b>\n\n" +
                   "💵 Naqd va 💳 kartadagi pulni kiritishingiz mumkin.";
            keyboard = InlineKeyboardMarkup.builder()
                    .inlineKeyboard(List.of(
                            List.of(InlineKeyboardButton.builder().text("✅ Foydani kiritish").callbackData("profit:enter").build()),
                            List.of(InlineKeyboardButton.builder().text("⏰ Keyinroq").callbackData("reminder:dismiss").build())
                    ))
                    .build();
        }

        apiClient.sendMessage(user.getTelegramId(), text, keyboard, "HTML");
        deliveryLogRepository.save(ReminderDeliveryLog.builder()
                .user(user)
                .reminderType(ReminderType.PROFIT_18)
                .reminderDate(today)
                .build());
        log.info("Sent 18:00 profit reminder to user {}", user.getId());
    }

    private void sendReminder21IfEligible(User user, LocalDate today) {
        if (deliveryLogRepository.existsByUserIdAndReminderTypeAndReminderDate(user.getId(), ReminderType.PROFIT_21, today)) {
            return;
        }

        Optional<DailyProfit> profitOpt = dailyProfitService.getProfit(user.getId(), today);
        String text;

        if (profitOpt.isPresent() && profitOpt.get().getTotalProfit() != null && profitOpt.get().getTotalProfit().compareTo(java.math.BigDecimal.ZERO) > 0) {
            text = String.format("""
                    🌙 <b>Kechki hisob-kitob:</b>

                    Bugungi kiritilgan daromadingiz (foydangiz): <b>%s</b>

                    Bugun yana qo‘shimcha daromad bo‘ldimi yoki yakuniy foydani kiritmoqchimisiz?
                    """,
                    MoneyFormatter.format(profitOpt.get().getTotalProfit())
            );
        } else {
            text = """
                    🌙 <b>Kechki hisob-kitob:</b>

                    Bugungi daromadingizni (foydangizni) kiriting!
                    Kunlik to‘liq hisobingiz chiqishi uchun daromad va qo‘lingizdagi foydani kiritishni unutmang.
                    """;
        }

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(InlineKeyboardButton.builder().text("💵 Daromadni / Foydani kiritish").callbackData("profit:enter").build()),
                        List.of(InlineKeyboardButton.builder().text("📊 Bugungi hisob").callbackData("report:daily").build())
                ))
                .build();

        apiClient.sendMessage(user.getTelegramId(), text, keyboard, "HTML");
        deliveryLogRepository.save(ReminderDeliveryLog.builder()
                .user(user)
                .reminderType(ReminderType.PROFIT_21)
                .reminderDate(today)
                .build());
        log.info("Sent 21:00 profit reminder to user {}", user.getId());
    }
}
