package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.entity.CategoryType;
import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.common.exception.ValidationException;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.service.DebtDraftService;
import com.hisobchi.bot.debt.service.DebtService;
import com.hisobchi.bot.idempotency.service.IdempotencyService;
import com.hisobchi.bot.notification.service.NotificationSettingsService;
import com.hisobchi.bot.report.service.ReportService;
import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.service.StatisticsService;
import com.hisobchi.bot.summary.service.DailySummaryService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory;
import com.hisobchi.bot.transaction.dto.DraftDto;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionDraft;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.service.TransactionDraftService;
import com.hisobchi.bot.transaction.service.TransactionService;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserState;
import com.hisobchi.bot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private final TelegramApiClient apiClient;
    private final TransactionDraftService draftService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final StatisticsService statisticsService;
    private final DailySummaryService dailySummaryService;
    private final UserService userService;
    private final InlineKeyboardFactory inlineKeyboardFactory;
    private final ReplyKeyboardFactory replyKeyboardFactory;
    private final IdempotencyService idempotencyService;
    private final DebtService debtService;
    private final DebtDraftService debtDraftService;
    private final NotificationSettingsService notificationSettingsService;
    private final ReportService reportService;
    private final TextMessageHandler textMessageHandler;

    public void handle(User user, CallbackQuery callback) {
        String data = callback.getData();
        if (data == null || data.isBlank()) return;

        Long chatId = callback.getMessage().getChat().getId();
        Integer messageId = callback.getMessage().getMessageId();
        String callbackId = callback.getId();

        // Immediate answer to remove loading spinner in Telegram client
        apiClient.answerCallbackQuery(callbackId, null, false);

        String[] parts = data.split(":");
        String prefix = parts[0];

        switch (prefix) {
            case "draft" -> handleDraftCallback(user, chatId, messageId, parts);
            case "day" -> handleDayCallback(user, chatId, messageId, parts);
            case "tx" -> handleTransactionCallback(user, chatId, messageId, parts);
            case "debt" -> handleDebtCallback(user, chatId, messageId, parts);
            case "notif" -> handleNotifCallback(user, chatId, messageId, parts);
            case "profit" -> handleProfitCallback(user, chatId, messageId, parts);
            case "reminder" -> handleReminderCallback(user, chatId, messageId, parts);
            case "report" -> handleReportCallback(user, chatId, messageId, parts);
            case "history" -> {
                if ("back".equals(parts[1])) {
                    apiClient.sendMessage(chatId, "📜 Tarix bo‘limi:", replyKeyboardFactory.getHistoryMenu(), null);
                }
            }
            default -> log.warn("Unknown callback query: {}", data);
        }
    }

    private void handleDebtCallback(User user, Long chatId, Integer messageId, String[] parts) {
        String action = parts[1];
        Long id = (parts.length > 2 && !parts[2].isBlank() && !"cancel_ext".equals(action)) ? Long.parseLong(parts[2]) : null;

        switch (action) {
            case "save" -> {
                String actionKey = "save_debt_draft_" + id;
                if (!idempotencyService.tryAcquireAction(actionKey)) return;

                var draftOpt = debtDraftService.findValidDraft(id, user.getId());
                if (draftOpt.isPresent()) {
                    Debt saved = debtService.saveFromDraft(draftOpt.get());
                    String msg = String.format("""
                            ✅ <b>Qarz saqlandi!</b>

                            %s
                            💰 <b>%s</b>
                            👤 <b>%s</b>
                            💳 Pul turi: <b>%s</b>
                            %s
                            """,
                            saved.getType() == DebtType.BORROWED ? "🔴 <b>Olingan qarz</b>" : "🟢 <b>Berilgan qarz</b>",
                            MoneyFormatter.format(saved.getAmount()),
                            BotMessageBuilder.escapeHtml(saved.getPersonName()),
                            saved.getPaymentMethod(),
                            saved.getDueDate() != null ? "📅 Muddati: " + saved.getDueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : ""
                    );
                    apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
                } else {
                    apiClient.editMessageText(chatId, messageId, "⚠️ Qarz drafti topilmadi yoki muddati o‘tgan.", null, null);
                }
            }
            case "cancel" -> {
                debtDraftService.deleteDraft(id);
                apiClient.editMessageText(chatId, messageId, "❌ Bekor qilindi.", null, null);
            }
            case "pay" -> {
                var debtOpt = debtService.getDebt(id, user.getId());
                if (debtOpt.isPresent()) {
                    Debt d = debtOpt.get();
                    debtService.markAsResolved(id, user.getId());
                    String todayStr = DateTimeUtils.today(user.getTimezone()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    String msg = d.getType() == DebtType.BORROWED
                            ? String.format("""
                            ✅ <b>Qarz yopildi</b>

                            👤 <b>%s</b>
                            💰 <b>%s</b>

                            📅 To‘landi:
                            <b>%s</b>
                            """, BotMessageBuilder.escapeHtml(d.getPersonName()), MoneyFormatter.format(d.getAmount()), todayStr)
                            : String.format("""
                            ✅ <b>Qarz qaytarib olindi</b>

                            👤 <b>%s</b>
                            💰 <b>%s</b>

                            📅 Qabul qilindi:
                            <b>%s</b>
                            """, BotMessageBuilder.escapeHtml(d.getPersonName()), MoneyFormatter.format(d.getAmount()), todayStr);
                    apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
                }
            }
            case "extend" -> {
                textMessageHandler.setExtendingDebtId(user.getId(), id);
                userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_EXTEND_DATE);
                apiClient.sendMessage(chatId, "📅 <b>Yangi qaytarish sanasini kiriting:</b>\n<i>Masalan: 25 oktabr, keyingi hafta, ertaga</i>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case "conf_ext" -> {
                LocalDate newDate = LocalDate.parse(parts[3]);
                debtService.extendDueDate(id, user.getId(), newDate);
                String msg = "✅ <b>Qaytarish sanasi " + newDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " ga uzaytirildi.</b>";
                apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
            }
            case "cancel_ext" -> {
                apiClient.editMessageText(chatId, messageId, "❌ Muddat uzaytirish bekor qilindi.", null, null);
            }
            case "dismiss" -> {
                apiClient.editMessageText(chatId, messageId, "👍 Tushunarli, eslatib turamiz.", null, null);
            }
            case "view" -> {
                var debtOpt = debtService.getDebt(id, user.getId());
                if (debtOpt.isPresent()) {
                    Debt d = debtOpt.get();
                    String msg = BotMessageBuilder.buildDebtDetailMessage(d);
                    apiClient.editMessageText(chatId, messageId, msg, inlineKeyboardFactory.getDebtActionKeyboard(d), "HTML");
                }
            }
            case "back" -> {
                List<Debt> borrowed = debtService.getActiveDebts(user.getId(), DebtType.BORROWED);
                List<Debt> lent = debtService.getActiveDebts(user.getId(), DebtType.LENT);
                String msg = BotMessageBuilder.buildActiveDebtsOverviewMessage(borrowed, lent);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getDebtsMenu(), "HTML");
            }
            case "del" -> {
                boolean ok = debtService.deleteDebt(id, user.getId());
                if (ok) {
                    apiClient.editMessageText(chatId, messageId, "🗑 <b>Qarz o‘chirildi.</b>", null, "HTML");
                }
            }
            case "edit_field" -> {
                String field = parts[3];
                textMessageHandler.setActiveDebtDraftForEdit(user.getId(), id);
                switch (field) {
                    case "amount" -> {
                        userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_EDIT_AMOUNT);
                        apiClient.sendMessage(chatId, "Yangi qarz summasini kiriting:", replyKeyboardFactory.getCancelMenu(), null);
                    }
                    case "person" -> {
                        userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_EDIT_PERSON);
                        apiClient.sendMessage(chatId, "Yangi shaxs ismini kiriting:", replyKeyboardFactory.getCancelMenu(), null);
                    }
                    case "date" -> {
                        userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_EDIT_DATE);
                        apiClient.sendMessage(chatId, "Yangi qaytarish muddatini kiriting (masalan: 15 oktabr):", replyKeyboardFactory.getCancelMenu(), null);
                    }
                }
            }
        }
    }

    private void handleNotifCallback(User user, Long chatId, Integer messageId, String[] parts) {
        String action = parts[1];
        if ("toggle".equals(action)) {
            String key = parts[2];
            var settings = notificationSettingsService.toggleSetting(user, key);
            var markup = inlineKeyboardFactory.getNotificationSettingsKeyboard(settings);
            apiClient.editMessageText(chatId, messageId,
                    "🔔 <b>Avtomatik eslatmalar va hisobotlar sozlamalari:</b>\n\nKerakli bandni yoqish yoki o‘chirish uchun ustiga bosing:",
                    markup, "HTML");
        } else if ("back".equals(action)) {
            apiClient.sendMessage(chatId, "⚙️ Sozlamalar menyusi:", replyKeyboardFactory.getSettingsMenu(), null);
        }
    }

    private void handleProfitCallback(User user, Long chatId, Integer messageId, String[] parts) {
        if ("enter".equals(parts[1])) {
            userService.updateState(user.getTelegramId(), UserState.WAITING_DAILY_PROFIT_CASH);
            apiClient.sendMessage(chatId,
                    "💵 <b>Bugungi naqd puldagi foydani kiriting:</b>\n<i>Masalan: 120000 yoki 120 ming</i>\n(Agar naqd bo'lmasa <code>0</code> deb yozing)",
                    replyKeyboardFactory.getCancelMenu(), "HTML");
        }
    }

    private void handleReminderCallback(User user, Long chatId, Integer messageId, String[] parts) {
        if ("dismiss".equals(parts[1])) {
            apiClient.editMessageText(chatId, messageId, "⏰ Eslatma keyinroqqa qoldirildi.", null, null);
        }
    }

    private void handleReportCallback(User user, Long chatId, Integer messageId, String[] parts) {
        if ("daily".equals(parts[1])) {
            LocalDate today = DateTimeUtils.today(user.getTimezone());
            var data = reportService.getDailyReportData(user.getId(), today);
            String msg = reportService.formatDailyReport(data);
            apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getMainMenu(), "HTML");
        }
    }

    private void handleDraftCallback(User user, Long chatId, Integer messageId, String[] parts) {
        String action = parts[1];
        Long draftId = Long.parseLong(parts[2]);

        switch (action) {
            case "save" -> {
                // Idempotency check: prevent duplicate saves on double click
                String actionKey = "save_draft_" + draftId;
                if (!idempotencyService.tryAcquireAction(actionKey)) {
                    log.warn("Duplicate save ignored for draft {}", draftId);
                    return;
                }

                try {
                    TransactionDto saved = transactionService.confirmAndSave(draftId, user.getId());
                    LocalDate today = DateTimeUtils.today(user.getTimezone());
                    DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

                    String successMsg = BotMessageBuilder.buildSaveSuccessMessage(
                            saved, stats.totalExpense(), stats.totalIncome(), stats.netProfit());

                    apiClient.editMessageText(chatId, messageId, successMsg, null, "HTML");
                    userService.updateState(user.getTelegramId(), UserState.IDLE);

                } catch (ValidationException e) {
                    apiClient.sendMessage(chatId, "⚠️ " + e.getMessage(), replyKeyboardFactory.getMainMenu(), null);
                } catch (Exception e) {
                    log.error("Failed to save draft {}: {}", draftId, e.getMessage(), e);
                    apiClient.sendMessage(chatId, "⚠️ Xatolik yuz berdi. Qaytadan urinib ko‘ring.",
                            replyKeyboardFactory.getMainMenu(), null);
                }
            }
            case "cancel" -> {
                draftService.cancelDraft(draftId, user.getId());
                apiClient.editMessageText(chatId, messageId, "❌ Bekor qilindi.", null, null);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case "edit" -> {
                apiClient.editMessageText(chatId, messageId, "Qaysi maydonni o‘zgartirmoqchisiz?",
                        inlineKeyboardFactory.getEditDraftFieldsKeyboard(draftId), null);
            }
            case "edit_field" -> {
                String field = parts[3];
                switch (field) {
                    case "amount" -> {
                        userService.updateState(user.getTelegramId(), UserState.WAITING_AMOUNT_EDIT);
                        apiClient.sendMessage(chatId, "Yangi summani kiriting (masalan: <code>20000</code>):",
                                replyKeyboardFactory.getCancelMenu(), "HTML");
                    }
                    case "cat" -> {
                        TransactionDraft d = draftService.getDraft(draftId, user.getId());
                        List<Category> categories = categoryService.getCategories(user.getId(), d.getType());
                        apiClient.editMessageText(chatId, messageId, "Yangi kategoriyani tanlang:",
                                inlineKeyboardFactory.getCategorySelectionKeyboard(draftId, categories), null);
                    }
                    case "desc" -> {
                        userService.updateState(user.getTelegramId(), UserState.WAITING_DESCRIPTION_EDIT);
                        apiClient.sendMessage(chatId, "Yangi izohni kiriting:", replyKeyboardFactory.getCancelMenu(), null);
                    }
                }
            }
            case "set_cat" -> {
                Long categoryId = Long.parseLong(parts[3]);
                Category cat = categoryService.getById(categoryId, user.getId());
                TransactionDraft draft = draftService.updateDraftCategory(draftId, user.getId(), cat);
                DraftDto dto = draftService.toDto(draft);
                String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone());
                apiClient.editMessageText(chatId, messageId, msg,
                        inlineKeyboardFactory.getDraftConfirmationKeyboard(draftId), "HTML");
            }
            case "back" -> {
                TransactionDraft d = draftService.getDraft(draftId, user.getId());
                DraftDto dto = draftService.toDto(d);
                String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone());
                apiClient.editMessageText(chatId, messageId, msg,
                        inlineKeyboardFactory.getDraftConfirmationKeyboard(draftId), "HTML");
            }
            case "intent" -> {
                String intentType = parts[3];
                TransactionType type = TransactionType.valueOf(intentType);
                draftService.updateDraftType(draftId, user.getId(), type);
                List<Category> categories = categoryService.getCategories(user.getId(), type);
                apiClient.editMessageText(chatId, messageId, "Kategoriyani tanlang:",
                        inlineKeyboardFactory.getCategorySelectionKeyboard(draftId, categories), null);
            }
        }
    }

    private void handleDayCallback(User user, Long chatId, Integer messageId, String[] parts) {
        String action = parts[1];
        LocalDate today = DateTimeUtils.today(user.getTimezone());

        switch (action) {
            case "close" -> {
                String sub = parts[2];
                if ("confirm".equals(sub)) {
                    DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);
                    var summary = dailySummaryService.closeDay(user, today, stats.totalIncome(), stats.totalExpense());
                    String msg = String.format("""
                            🔐 <b>Kun yakunlandi va yopildi!</b>
                            ━━━━━━━━━━━━━━━━━━
                            📅 Sana: <b>%s</b>

                            💰 Jami daromad:
                            <b>%s</b>

                            💸 Jami xarajat:
                            <b>%s</b>

                            ━━━━━━━━━━━━━━━━━━
                            ✅ <b>SOF FOYDA:</b>
                            <b>%s</b>
                            ━━━━━━━━━━━━━━━━━━
                            """,
                            DateTimeUtils.formatDate(today),
                            MoneyFormatter.format(summary.getTotalIncome()),
                            MoneyFormatter.format(summary.getTotalExpense()),
                            MoneyFormatter.format(summary.getNetProfit())
                    );
                    apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
                } else {
                    apiClient.editMessageText(chatId, messageId, "Kunni yopish bekor qilindi.", null, null);
                }
            }
            case "reopen" -> {
                String sub = parts[2];
                if ("confirm".equals(sub)) {
                    LocalDate date = LocalDate.parse(parts[3]);
                    dailySummaryService.reopenDay(user.getId(), date);
                    apiClient.editMessageText(chatId, messageId,
                            "🔓 <b>" + DateTimeUtils.formatDate(date) + "</b> kuni qayta ochildi.", null, "HTML");
                } else {
                    apiClient.editMessageText(chatId, messageId, "Bekor qilindi.", null, null);
                }
            }
        }
    }

    private void handleTransactionCallback(User user, Long chatId, Integer messageId, String[] parts) {
        String action = parts[1];
        Long txId = Long.parseLong(parts[2]);

        switch (action) {
            case "detail" -> {
                Transaction tx = transactionService.getByIdAndUser(txId, user.getId());
                TransactionDto dto = transactionService.toDto(tx);
                String msg = BotMessageBuilder.buildTransactionDetail(dto, user.getTimezone());
                apiClient.editMessageText(chatId, messageId, msg,
                        inlineKeyboardFactory.getHistoryItemActionsKeyboard(txId), "HTML");
            }
            case "delete_ask" -> {
                apiClient.editMessageText(chatId, messageId,
                        "⚠️ <b>Ushbu operatsiyani o‘chirmoqchimisiz?</b>",
                        inlineKeyboardFactory.getDeleteConfirmationKeyboard(txId), "HTML");
            }
            case "delete_confirm" -> {
                transactionService.deleteTransaction(txId, user.getId());
                apiClient.editMessageText(chatId, messageId, "🗑 <b>Operatsiya o‘chirildi.</b>", null, "HTML");
            }
            case "delete_cancel" -> {
                Transaction tx = transactionService.getByIdAndUser(txId, user.getId());
                TransactionDto dto = transactionService.toDto(tx);
                String msg = BotMessageBuilder.buildTransactionDetail(dto, user.getTimezone());
                apiClient.editMessageText(chatId, messageId, msg,
                        inlineKeyboardFactory.getHistoryItemActionsKeyboard(txId), "HTML");
            }
            case "edit" -> {
                apiClient.editMessageText(chatId, messageId, "Qaysi maydonni tahrirlaysiz?",
                        inlineKeyboardFactory.getEditTransactionFieldsKeyboard(txId), null);
            }
        }
    }
}
