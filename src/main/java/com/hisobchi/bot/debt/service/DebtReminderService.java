package com.hisobchi.bot.debt.service;

import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.debt.catalog.DebtHadithCatalog;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtReminderLog;
import com.hisobchi.bot.debt.entity.DebtStatus;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtReminderLogRepository;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtReminderService {

    private final DebtRepository debtRepository;
    private final DebtReminderLogRepository reminderLogRepository;
    private final UserRepository userRepository;
    private final TelegramApiClient telegramClient;
    private final InlineKeyboardFactory inlineKeyboardFactory;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d-MMMM", java.util.Locale.forLanguageTag("uz-UZ"));

    @Transactional
    public void processDebtReminders() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                processRemindersForUser(user);
            } catch (Exception e) {
                log.error("Error processing debt reminders for user {}", user.getId(), e);
            }
        }
    }

    private void processRemindersForUser(User user) {
        ZoneId zoneId = DateTimeUtils.resolveZone(user.getTimezone());
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        int hour = now.getHour();
        int minute = now.getMinute();
        LocalDate today = now.toLocalDate();

        // Check active debts for this user
        List<Debt> activeDebts = debtRepository.findByUserIdAndStatusInOrderByDueDateAscCreatedAtDesc(
                user.getId(), List.of(DebtStatus.ACTIVE, DebtStatus.OVERDUE));

        for (Debt debt : activeDebts) {
            if (debt.getDueDate() == null) continue;

            // 1. Two days before due date (dueDate == today + 2)
            if (debt.getDueDate().equals(today.plusDays(2)) && hour == 10 && minute == 0) {
                sendTwoDaysBeforeReminder(user, debt, today);
            }

            // 2. Due date today (dueDate == today)
            if (debt.getDueDate().equals(today)) {
                sendDueDayReminder(user, debt, today, hour, minute);
            }

            // 3. Overdue check (dueDate < today) at 11:00
            if (debt.getDueDate().isBefore(today) && hour == 11 && minute == 0) {
                sendOverdueReminder(user, debt, today);
            }
        }
    }

    private void sendTwoDaysBeforeReminder(User user, Debt debt, LocalDate today) {
        String reminderType = debt.getType() == DebtType.BORROWED ? "BORROWED_TWO_DAYS_BEFORE" : "LENT_TWO_DAYS_BEFORE";
        if (reminderLogRepository.existsByDebtIdAndReminderTypeAndScheduledDate(debt.getId(), reminderType, today)) {
            return;
        }

        String msg;
        InlineKeyboardMarkup kb;

        if (debt.getType() == DebtType.BORROWED) {
            msg = String.format(
                    "⏰ <b>QARZ ESLATMASI</b>\n\n" +
                    "Siz <b>%s</b>dan olgan qarzingizni qaytarishga:\n" +
                    "<b>2 kun qoldi.</b>\n\n" +
                    "🔴 <b>Qolgan qarz:</b>\n%s\n\n" +
                    "📅 <b>To‘lash sanasi:</b>\n%s\n\n" +
                    "Qarzni o‘z vaqtida qaytarishni rejalashtirib qo‘ying.\n\n" +
                    "%s",
                    debt.getPersonName(),
                    MoneyFormatter.format(debt.getRemainingAmount()),
                    debt.getDueDate().format(DATE_FMT),
                    DebtHadithCatalog.getBorrowedHadith()
            );
            kb = inlineKeyboardFactory.getBorrowedReminderKeyboard(debt.getId());
        } else {
            msg = String.format(
                    "⏰ <b>QARZ ESLATMASI</b>\n\n" +
                    "<b>%s</b>ga bergan qarzingizning qaytarilishiga:\n" +
                    "<b>2 kun qoldi.</b>\n\n" +
                    "🟢 <b>Qolgan qarzi:</b>\n%s\n\n" +
                    "📅 <b>Qaytarish sanasi:</b>\n%s\n\n" +
                    "%s",
                    debt.getPersonName(),
                    MoneyFormatter.format(debt.getRemainingAmount()),
                    debt.getDueDate().format(DATE_FMT),
                    DebtHadithCatalog.getLentRespiteHadith()
            );
            kb = inlineKeyboardFactory.getLentReminderKeyboard(debt.getId());
        }

        sendAndLog(user, debt, reminderType, today, msg, kb);
    }

    private void sendDueDayReminder(User user, Debt debt, LocalDate today, int hour, int minute) {
        if (minute != 0) return;

        if (debt.getType() == DebtType.BORROWED) {
            // Hours: 09:00, 13:00, 17:00, 21:00
            String typeSlot = switch (hour) {
                case 9 -> "BORROWED_DUE_09";
                case 13 -> "BORROWED_DUE_13";
                case 17 -> "BORROWED_DUE_17";
                case 21 -> "BORROWED_DUE_21";
                default -> null;
            };
            if (typeSlot == null) return;
            if (reminderLogRepository.existsByDebtIdAndReminderTypeAndScheduledDate(debt.getId(), typeSlot, today)) {
                return;
            }

            String msg;
            if (hour == 9) {
                msg = String.format(
                        "⏰ <b>Bugun %sga qarzni to‘lash kuni.</b>\n\n" +
                        "🔴 <b>Qolgan:</b>\n%s\n\n" +
                        "To‘ladingizmi?\n\n" +
                        "%s",
                        debt.getPersonName(),
                        MoneyFormatter.format(debt.getRemainingAmount()),
                        DebtHadithCatalog.getBorrowedHadith()
                );
            } else {
                msg = String.format(
                        "🔔 <b>Eslatma</b>\n\n" +
                        "<b>%s</b>ga bo‘lgan <b>%s</b> qarzingiz bugun to‘lanishi kerak.\n\n" +
                        "To‘ladingizmi?",
                        debt.getPersonName(),
                        MoneyFormatter.format(debt.getRemainingAmount())
                );
            }

            InlineKeyboardMarkup kb = inlineKeyboardFactory.getBorrowedReminderKeyboard(debt.getId());
            sendAndLog(user, debt, typeSlot, today, msg, kb);
        } else {
            // LENT hours: 09:00, 14:00, 19:00
            String typeSlot = switch (hour) {
                case 9 -> "LENT_DUE_09";
                case 14 -> "LENT_DUE_14";
                case 19 -> "LENT_DUE_19";
                default -> null;
            };
            if (typeSlot == null) return;
            if (reminderLogRepository.existsByDebtIdAndReminderTypeAndScheduledDate(debt.getId(), typeSlot, today)) {
                return;
            }

            String msg = String.format(
                    "🤝 <b>Bugun %sning qarzni qaytarish kuni.</b>\n\n" +
                    "💰 <b>Qolgan qarzi:</b>\n%s\n\n" +
                    "Qarz qaytdimi?\n\n" +
                    "%s",
                    debt.getPersonName(),
                    MoneyFormatter.format(debt.getRemainingAmount()),
                    DebtHadithCatalog.getLentRespiteHadith()
            );

            InlineKeyboardMarkup kb = inlineKeyboardFactory.getLentReminderKeyboard(debt.getId());
            sendAndLog(user, debt, typeSlot, today, msg, kb);
        }
    }

    private void sendOverdueReminder(User user, Debt debt, LocalDate today) {
        String reminderType = "OVERDUE_DAILY";
        if (reminderLogRepository.existsByDebtIdAndReminderTypeAndScheduledDate(debt.getId(), reminderType, today)) {
            return;
        }

        debt.setStatus(DebtStatus.OVERDUE);
        debtRepository.save(debt);

        String msg;
        InlineKeyboardMarkup kb;

        if (debt.getType() == DebtType.BORROWED) {
            msg = String.format(
                    "⚠️ <b>Qarz muddati o‘tdi</b>\n\n" +
                    "👤 <b>%s</b>\n" +
                    "💰 <b>%s</b>\n\n" +
                    "📅 <b>Qaytarish sanasi:</b> %s\n\n" +
                    "Qarz hali to‘lanmagan deb turibdi.\n\n" +
                    "To‘ladingizmi?",
                    debt.getPersonName(),
                    MoneyFormatter.format(debt.getAmount()),
                    debt.getDueDate().format(DATE_FMT)
            );
            kb = InlineKeyboardMarkup.builder()
                    .inlineKeyboard(List.of(
                            List.of(new InlineKeyboardButton("✅ To‘ladim", "debt:pay:" + debt.getId())),
                            List.of(new InlineKeyboardButton("📅 Yangi muddat", "debt:extend:" + debt.getId()))
                    ))
                    .build();
        } else {
            msg = String.format(
                    "⚠️ <b>Qarz muddati o‘tdi</b>\n\n" +
                    "👤 <b>%s</b>\n" +
                    "💰 <b>%s</b>\n\n" +
                    "📅 <b>Qaytarish sanasi:</b> %s\n\n" +
                    "Qarzingiz qaytarildimi?\n\n" +
                    "%s",
                    debt.getPersonName(),
                    MoneyFormatter.format(debt.getAmount()),
                    debt.getDueDate().format(DATE_FMT),
                    DebtHadithCatalog.getLentRespiteHadith()
            );
            kb = InlineKeyboardMarkup.builder()
                    .inlineKeyboard(List.of(
                            List.of(new InlineKeyboardButton("✅ Qaytarib oldim", "debt:pay:" + debt.getId())),
                            List.of(new InlineKeyboardButton("📅 Muddat berish", "debt:extend:" + debt.getId()))
                    ))
                    .build();
        }

        sendAndLog(user, debt, reminderType, today, msg, kb);
    }

    private void sendAndLog(User user, Debt debt, String reminderType, LocalDate date, String text, InlineKeyboardMarkup kb) {
        try {
            telegramClient.sendMessage(user.getTelegramId(), text, kb, "HTML");

            DebtReminderLog logEntry = DebtReminderLog.builder()
                    .debt(debt)
                    .user(user)
                    .reminderType(reminderType)
                    .scheduledDate(date)
                    .build();
            reminderLogRepository.save(logEntry);
            log.info("Sent debt reminder {} for debt {} to user {}", reminderType, debt.getId(), user.getTelegramId());
        } catch (Exception e) {
            log.error("Failed to send debt reminder {} for debt {}", reminderType, debt.getId(), e);
        }
    }
}
