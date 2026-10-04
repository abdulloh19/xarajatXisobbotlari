package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.entity.CategoryType;
import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.common.exception.ValidationException;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtDraft;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.service.DebtDraftService;
import com.hisobchi.bot.debt.service.DebtFlowService;
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
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.transaction.service.TransactionDraftService;
import com.hisobchi.bot.transaction.service.TransactionService;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserState;
import com.hisobchi.bot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private final TransactionRepository transactionRepository;
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
    private final com.hisobchi.bot.debt.service.DebtFlowService debtFlowService;
    private final com.hisobchi.bot.user.service.BalanceService balanceService;
    private final com.hisobchi.bot.profit.service.DailyProfitService dailyProfitService;

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
            case "debt_create" -> handleDebtCreateCallback(user, chatId, messageId, parts);
            case "debt_pay" -> handleDebtPayCallback(user, chatId, messageId, parts);
            case "debt_ret" -> handleDebtReturnCallback(user, chatId, messageId, parts);
            case "debt_voice" -> handleDebtVoiceCallback(user, chatId, messageId, parts);
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
                    BigDecimal available = balanceService.getAvailableBalance(user.getId());

                    String msg;
                    if (saved.getType() == DebtType.LENT) {
                        msg = String.format("""
                                ✅ <b>Qarz saqlandi</b>

                                👤 <b>%s</b>
                                💸 Qarz berdingiz:
                                <b>%s</b>

                                🤝 %sning qarzi:
                                <b>%s</b>

                                💰 Sizda hozir qoldi:
                                <b>%s</b>
                                """,
                                BotMessageBuilder.escapeHtml(saved.getPersonName()),
                                MoneyFormatter.format(saved.getAmount()),
                                BotMessageBuilder.escapeHtml(saved.getPersonName()),
                                MoneyFormatter.format(saved.getRemainingAmount()),
                                MoneyFormatter.format(available)
                        );
                    } else {
                        msg = String.format("""
                                ✅ <b>Qarz saqlandi</b>

                                👤 <b>%s</b>
                                💰 Qarz oldingiz:
                                <b>%s</b>

                                🤝 Sizning qarzingiz:
                                <b>%s</b>

                                💰 Sizda hozir mavjud:
                                <b>%s</b>
                                """,
                                BotMessageBuilder.escapeHtml(saved.getPersonName()),
                                MoneyFormatter.format(saved.getAmount()),
                                MoneyFormatter.format(saved.getRemainingAmount()),
                                MoneyFormatter.format(available)
                        );
                    }
                    apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
                } else {
                    apiClient.editMessageText(chatId, messageId, "⚠️ Qarz drafti topilmadi yoki muddati o‘tgan.", null, null);
                }
            }
            case "cancel" -> {
                debtDraftService.deleteDraft(id);
                apiClient.editMessageText(chatId, messageId, "❌ Bekor qilindi.", null, null);
            }
            case "history" -> {
                var debtOpt = debtService.getDebt(id, user.getId());
                if (debtOpt.isPresent()) {
                    Debt d = debtOpt.get();
                    var payments = debtService.getPaymentsForDebt(id, user.getId());
                    String msg = BotMessageBuilder.buildPaymentHistoryMessage(d, payments);
                    apiClient.editMessageText(chatId, messageId, msg, inlineKeyboardFactory.getDebtActionKeyboard(d), "HTML");
                }
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
                    String msg = BotMessageBuilder.buildDebtDetailPage(d);
                    apiClient.editMessageText(chatId, messageId, msg, inlineKeyboardFactory.getDebtDetailPageKeyboard(d), "HTML");
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
            case "edit_ask" -> {
                apiClient.editMessageText(chatId, messageId, "Qaysi maydonni tahrirlaysiz?",
                        inlineKeyboardFactory.getDebtCreateEditFieldsKeyboard(id), null);
            }
            case "edit_back" -> {
                debtDraftService.findValidDraft(id, user.getId()).ifPresent(d -> {
                    String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(d);
                    apiClient.editMessageText(chatId, messageId, confirmMsg,
                            inlineKeyboardFactory.getDebtConfirmationKeyboard(d.getId()), "HTML");
                });
            }
            case "cancel_create" -> {
                textMessageHandler.clearUserDebtCreation(user.getId());
                apiClient.editMessageText(chatId, messageId, "❌ Qarz yaratish bekor qilindi.", null, null);
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

    private void handleDebtCreateCallback(User user, Long chatId, Integer messageId, String[] parts) {
        if (parts.length < 3) return;
        String action = parts[1];
        if ("method".equals(action)) {
            String methodKey = parts[2];
            String paymentMethod = "card".equalsIgnoreCase(methodKey) ? "Karta" : "Naqd";
            BigDecimal amount = textMessageHandler.removeUserDebtAmount(user.getId());
            String person = textMessageHandler.removeUserDebtPerson(user.getId());
            LocalDate dueDate = textMessageHandler.removeUserDebtDate(user.getId());
            DebtType type = textMessageHandler.removeUserDebtType(user.getId());
            if (type == null) type = DebtType.LENT;
            if (amount == null) amount = BigDecimal.ZERO;
            if (person == null) person = "Noma'lum";

            DebtDraft draft = debtDraftService.createDraft(
                    user, type, amount, person, dueDate, null, null, 1.0, paymentMethod, LocalDate.now()
            );

            String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(draft);
            apiClient.editMessageText(chatId, messageId, confirmMsg,
                    inlineKeyboardFactory.getDebtConfirmationKeyboard(draft.getId()), "HTML");
        }
    }

    private void handleDebtPayCallback(User user, Long chatId, Integer messageId, String[] parts) {
        if (parts.length < 2) return;
        String action = parts[1];

        switch (action) {
            case "start", "select" -> {
                Long debtId = parts.length > 2 ? Long.parseLong(parts[2]) : null;
                if (debtId == null) {
                    List<Debt> borrowed = debtService.getActiveDebts(user.getId(), DebtType.BORROWED);
                    var grouped = borrowed.stream().collect(java.util.stream.Collectors.groupingBy(Debt::getPersonName));
                    apiClient.editMessageText(chatId, messageId, "Kimga qarz to‘lamoqchisiz?",
                            inlineKeyboardFactory.getDebtGroupedSelectionKeyboard(grouped, "debt_pay"), null);
                    return;
                }
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.PAY_BORROWED, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    String prompt = String.format("%sga qancha bermoqchisiz?\n\nQolgan qarz:\n<b>%s</b>",
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount()));
                    apiClient.editMessageText(chatId, messageId, prompt,
                            inlineKeyboardFactory.getDebtAmountChoiceKeyboard(debtId, debt.getRemainingAmount(), "debt_pay"), "HTML");
                }
            }
            case "person" -> {
                String person = parts[2];
                List<Debt> debts = debtService.getActiveDebts(user.getId(), DebtType.BORROWED).stream()
                        .filter(d -> d.getPersonName().equalsIgnoreCase(person)).toList();
                apiClient.editMessageText(chatId, messageId, "Qaysi qarzni to‘lamoqchisiz?",
                        inlineKeyboardFactory.getDebtSubSelectionKeyboard(debts, "debt_pay"), null);
            }
            case "back_persons", "back" -> {
                List<Debt> borrowed = debtService.getActiveDebts(user.getId(), DebtType.BORROWED);
                var grouped = borrowed.stream().collect(java.util.stream.Collectors.groupingBy(Debt::getPersonName));
                apiClient.editMessageText(chatId, messageId, "Kimga qarz to‘lamoqchisiz?",
                        inlineKeyboardFactory.getDebtGroupedSelectionKeyboard(grouped, "debt_pay"), null);
            }
            case "full", "full_start" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.PAY_BORROWED, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    var session = debtFlowService.getSession(user.getId());
                    session.setPaymentAmount(debt.getRemainingAmount());
                    session.setFull(true);
                    apiClient.editMessageText(chatId, messageId, "Pul qayerdan beriladi?",
                            inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debtId, "debt_pay"), null);
                }
            }
            case "custom" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.PAY_BORROWED, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_PAY_AMOUNT);
                    apiClient.sendMessage(chatId, "💰 <b>To‘laydigan summani kiriting:</b>\n\nMasalan:\n<code>500000</code>",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                }
            }
            case "back_amt" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    String prompt = String.format("%sga qancha bermoqchisiz?\n\nQolgan qarz:\n<b>%s</b>",
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount()));
                    apiClient.editMessageText(chatId, messageId, prompt,
                            inlineKeyboardFactory.getDebtAmountChoiceKeyboard(debtId, debt.getRemainingAmount(), "debt_pay"), "HTML");
                }
            }
            case "method" -> {
                Long debtId = Long.parseLong(parts[2]);
                String methodKey = parts[3];
                String method = "card".equalsIgnoreCase(methodKey) ? "Karta" : "Naqd";
                var session = debtFlowService.getSession(user.getId());
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent() && session != null) {
                    Debt debt = debtOpt.get();
                    session.setPaymentMethod(method);
                    if (session.isFull()) {
                        String confirmMsg = String.format("""
                                👤 <b>%s</b>

                                💳 To‘liq qarz to‘lovi:
                                <b>%s</b>

                                💵 To‘lov turi: <b>%s</b>

                                Shundan keyin bu qarz to‘liq yopiladi.

                                Davom etamizmi?
                                """,
                                BotMessageBuilder.escapeHtml(debt.getPersonName()),
                                MoneyFormatter.format(session.getPaymentAmount()),
                                method
                        );
                        apiClient.editMessageText(chatId, messageId, confirmMsg,
                                inlineKeyboardFactory.getDebtFullPaymentConfirmationKeyboard(debtId, "debt_pay", true), "HTML");
                    } else {
                        BigDecimal payAmt = session.getPaymentAmount();
                        BigDecimal remainingAfter = debt.getRemainingAmount().subtract(payAmt);
                        String confirmMsg = String.format("""
                                💳 <b>QARZ TO‘LASH</b>

                                👤 <b>%s</b>

                                💰 Hozir to‘laysiz:
                                <b>%s</b>

                                📌 Oldingi qarz:
                                <b>%s</b>

                                ✅ To‘lovdan keyin qoladi:
                                <b>%s</b>

                                💵 To‘lov turi: <b>%s</b>

                                Tasdiqlaysizmi?
                                """,
                                BotMessageBuilder.escapeHtml(debt.getPersonName()),
                                MoneyFormatter.format(payAmt),
                                MoneyFormatter.format(debt.getRemainingAmount()),
                                MoneyFormatter.format(remainingAfter),
                                method
                        );
                        apiClient.editMessageText(chatId, messageId, confirmMsg,
                                inlineKeyboardFactory.getDebtPaymentConfirmationKeyboard(debtId, "debt_pay", true), "HTML");
                    }
                }
            }
            case "confirm" -> {
                Long debtId = Long.parseLong(parts[2]);
                String actionKey = "debt_pay_confirm_" + debtId + "_" + messageId;
                if (!idempotencyService.tryAcquireAction(actionKey)) return;

                var session = debtFlowService.getSession(user.getId());
                String method = (session != null && session.getPaymentMethod() != null) ? session.getPaymentMethod() : "Naqd";
                BigDecimal payAmt = (session != null && session.getPaymentAmount() != null) ? session.getPaymentAmount() : BigDecimal.ZERO;
                com.hisobchi.bot.debt.entity.DebtPayment payment = debtService.makePartialPayment(debtId, user.getId(), payAmt, method, TransactionSource.MANUAL);
                Debt updated = payment.getDebt();
                BigDecimal available = balanceService.getAvailableBalance(user.getId());
                debtFlowService.clearSession(user.getId());

                String msg = String.format("""
                        ✅ <b>To‘lov saqlandi</b>

                        👤 <b>%s</b>

                        💳 To‘ladingiz:
                        <b>%s</b>

                        📌 Oldingi qarz:
                        <b>%s</b>

                        🔴 Endi qolgan qarz:
                        <b>%s</b>

                        💰 Sizda mavjud:
                        <b>%s</b>
                        """,
                        BotMessageBuilder.escapeHtml(updated.getPersonName()),
                        MoneyFormatter.format(payAmt),
                        MoneyFormatter.format(updated.getRemainingAmount().add(payAmt)),
                        MoneyFormatter.format(updated.getRemainingAmount()),
                        MoneyFormatter.format(available)
                );
                apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
            }
            case "confirm_full" -> {
                Long debtId = Long.parseLong(parts[2]);
                String actionKey = "debt_pay_full_confirm_" + debtId + "_" + messageId;
                if (!idempotencyService.tryAcquireAction(actionKey)) return;

                var session = debtFlowService.getSession(user.getId());
                String method = (session != null && session.getPaymentMethod() != null) ? session.getPaymentMethod() : "Naqd";
                com.hisobchi.bot.debt.entity.DebtPayment payment = debtService.makeFullPayment(debtId, user.getId(), method, TransactionSource.MANUAL);
                Debt updated = payment.getDebt();
                BigDecimal available = balanceService.getAvailableBalance(user.getId());
                debtFlowService.clearSession(user.getId());

                String msg = String.format("""
                        ✅ <b>Qarz to‘liq yopildi</b>

                        👤 <b>%s</b>
                        💰 To‘landi: <b>%s</b>
                        📌 Qoldiq: <b>0 so‘m</b>

                        💰 Sizda mavjud:
                        <b>%s</b>
                        """,
                        BotMessageBuilder.escapeHtml(updated.getPersonName()),
                        MoneyFormatter.format(updated.getOriginalAmount()),
                        MoneyFormatter.format(available)
                );
                apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
            }
            case "cancel" -> {
                debtFlowService.clearSession(user.getId());
                apiClient.editMessageText(chatId, messageId, "❌ Bekor qilindi.", null, null);
            }
        }
    }

    private void handleDebtReturnCallback(User user, Long chatId, Integer messageId, String[] parts) {
        if (parts.length < 2) return;
        String action = parts[1];

        switch (action) {
            case "start", "select" -> {
                Long debtId = parts.length > 2 ? Long.parseLong(parts[2]) : null;
                if (debtId == null) {
                    List<Debt> lent = debtService.getActiveDebts(user.getId(), DebtType.LENT);
                    var grouped = lent.stream().collect(java.util.stream.Collectors.groupingBy(Debt::getPersonName));
                    apiClient.editMessageText(chatId, messageId, "Kim qarz qaytardi?",
                            inlineKeyboardFactory.getDebtGroupedSelectionKeyboard(grouped, "debt_ret"), null);
                    return;
                }
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.RETURN_LENT, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    String prompt = String.format("%s qancha qaytardi?\n\nQolgan qarzi:\n<b>%s</b>",
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount()));
                    apiClient.editMessageText(chatId, messageId, prompt,
                            inlineKeyboardFactory.getDebtAmountChoiceKeyboard(debtId, debt.getRemainingAmount(), "debt_ret"), "HTML");
                }
            }
            case "person" -> {
                String person = parts[2];
                List<Debt> debts = debtService.getActiveDebts(user.getId(), DebtType.LENT).stream()
                        .filter(d -> d.getPersonName().equalsIgnoreCase(person)).toList();
                apiClient.editMessageText(chatId, messageId, "Qaysi qarz qaytdi?",
                        inlineKeyboardFactory.getDebtSubSelectionKeyboard(debts, "debt_ret"), null);
            }
            case "back_persons", "back" -> {
                List<Debt> lent = debtService.getActiveDebts(user.getId(), DebtType.LENT);
                var grouped = lent.stream().collect(java.util.stream.Collectors.groupingBy(Debt::getPersonName));
                apiClient.editMessageText(chatId, messageId, "Kim qarz qaytardi?",
                        inlineKeyboardFactory.getDebtGroupedSelectionKeyboard(grouped, "debt_ret"), null);
            }
            case "full", "full_start" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.RETURN_LENT, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    var session = debtFlowService.getSession(user.getId());
                    session.setPaymentAmount(debt.getRemainingAmount());
                    session.setFull(true);
                    apiClient.editMessageText(chatId, messageId, "Pul qayerga tushdi?",
                            inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debtId, "debt_ret"), null);
                }
            }
            case "custom" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.RETURN_LENT, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_RETURN_AMOUNT);
                    apiClient.sendMessage(chatId, "💰 <b>Qancha qaytardi?</b>\n\nMasalan:\n<code>600000</code>",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                }
            }
            case "back_amt" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    String prompt = String.format("%s qancha qaytardi?\n\nQolgan qarzi:\n<b>%s</b>",
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount()));
                    apiClient.editMessageText(chatId, messageId, prompt,
                            inlineKeyboardFactory.getDebtAmountChoiceKeyboard(debtId, debt.getRemainingAmount(), "debt_ret"), "HTML");
                }
            }
            case "method" -> {
                Long debtId = Long.parseLong(parts[2]);
                String methodKey = parts[3];
                String method = "card".equalsIgnoreCase(methodKey) ? "Karta" : "Naqd";
                var session = debtFlowService.getSession(user.getId());
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent() && session != null) {
                    Debt debt = debtOpt.get();
                    session.setPaymentMethod(method);
                    if (session.isFull()) {
                        String confirmMsg = String.format("""
                                👤 <b>%s</b>

                                💵 To‘liq qarz qaytarildi:
                                <b>%s</b>

                                💳 To‘lov turi: <b>%s</b>

                                Shundan keyin bu qarz to‘liq yopiladi.

                                Saqlaymizmi?
                                """,
                                BotMessageBuilder.escapeHtml(debt.getPersonName()),
                                MoneyFormatter.format(session.getPaymentAmount()),
                                method
                        );
                        apiClient.editMessageText(chatId, messageId, confirmMsg,
                                inlineKeyboardFactory.getDebtFullPaymentConfirmationKeyboard(debtId, "debt_ret", false), "HTML");
                    } else {
                        BigDecimal payAmt = session.getPaymentAmount();
                        BigDecimal remainingAfter = debt.getRemainingAmount().subtract(payAmt);
                        String confirmMsg = String.format("""
                                💵 <b>QARZ QAYTARILDI</b>

                                👤 <b>%s</b>

                                💰 Qaytardi:
                                <b>%s</b>

                                📌 Oldingi qarz:
                                <b>%s</b>

                                ✅ Endi qoladi:
                                <b>%s</b>

                                💳 To‘lov turi: <b>%s</b>

                                Saqlaymizmi?
                                """,
                                BotMessageBuilder.escapeHtml(debt.getPersonName()),
                                MoneyFormatter.format(payAmt),
                                MoneyFormatter.format(debt.getRemainingAmount()),
                                MoneyFormatter.format(remainingAfter),
                                method
                        );
                        apiClient.editMessageText(chatId, messageId, confirmMsg,
                                inlineKeyboardFactory.getDebtPaymentConfirmationKeyboard(debtId, "debt_ret", false), "HTML");
                    }
                }
            }
            case "confirm" -> {
                Long debtId = Long.parseLong(parts[2]);
                String actionKey = "debt_ret_confirm_" + debtId + "_" + messageId;
                if (!idempotencyService.tryAcquireAction(actionKey)) return;

                var session = debtFlowService.getSession(user.getId());
                String method = (session != null && session.getPaymentMethod() != null) ? session.getPaymentMethod() : "Naqd";
                BigDecimal payAmt = (session != null && session.getPaymentAmount() != null) ? session.getPaymentAmount() : BigDecimal.ZERO;
                com.hisobchi.bot.debt.entity.DebtPayment payment = debtService.makePartialPayment(debtId, user.getId(), payAmt, method, TransactionSource.MANUAL);
                Debt updated = payment.getDebt();
                debtFlowService.clearSession(user.getId());

                String methodAddLine = "Karta".equalsIgnoreCase(method)
                        ? "💳 Kartangizga qo‘shildi: <b>" + MoneyFormatter.format(payAmt) + "</b>"
                        : "💵 Naqdingizga qo‘shildi: <b>" + MoneyFormatter.format(payAmt) + "</b>";

                String msg = String.format("""
                        ✅ <b>Qarz qaytarildi</b>

                        👤 <b>%s</b>

                        💰 Qaytardi:
                        <b>%s</b>

                        🟢 %sda endi qolgan:
                        <b>%s</b>

                        %s
                        """,
                        BotMessageBuilder.escapeHtml(updated.getPersonName()),
                        MoneyFormatter.format(payAmt),
                        BotMessageBuilder.escapeHtml(updated.getPersonName()),
                        MoneyFormatter.format(updated.getRemainingAmount()),
                        methodAddLine
                );
                apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
            }
            case "confirm_full" -> {
                Long debtId = Long.parseLong(parts[2]);
                String actionKey = "debt_ret_full_confirm_" + debtId + "_" + messageId;
                if (!idempotencyService.tryAcquireAction(actionKey)) return;

                var session = debtFlowService.getSession(user.getId());
                String method = (session != null && session.getPaymentMethod() != null) ? session.getPaymentMethod() : "Naqd";
                com.hisobchi.bot.debt.entity.DebtPayment payment = debtService.makeFullPayment(debtId, user.getId(), method, TransactionSource.MANUAL);
                Debt updated = payment.getDebt();
                debtFlowService.clearSession(user.getId());

                String methodAddLine = "Karta".equalsIgnoreCase(method)
                        ? "💳 Kartangizga qo‘shildi: <b>" + MoneyFormatter.format(updated.getOriginalAmount()) + "</b>"
                        : "💵 Naqdingizga qo‘shildi: <b>" + MoneyFormatter.format(updated.getOriginalAmount()) + "</b>";

                String msg = String.format("""
                        ✅ <b>%sning qarzi to‘liq yopildi.</b>

                        Qoldiq:
                        <b>0 so‘m</b>

                        %s
                        """,
                        BotMessageBuilder.escapeHtml(updated.getPersonName()),
                        methodAddLine
                );
                apiClient.editMessageText(chatId, messageId, msg, null, "HTML");
            }
            case "cancel" -> {
                debtFlowService.clearSession(user.getId());
                apiClient.editMessageText(chatId, messageId, "❌ Bekor qilindi.", null, null);
            }
        }
    }

    private void handleDebtVoiceCallback(User user, Long chatId, Integer messageId, String[] parts) {
        if (parts.length < 2) return;
        String action = parts[1];

        switch (action) {
            case "pay_full" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.PAY_BORROWED, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    var session = debtFlowService.getSession(user.getId());
                    session.setPaymentAmount(debt.getRemainingAmount());
                    session.setFull(true);
                    apiClient.editMessageText(chatId, messageId, "Pul qayerdan berildi?",
                            inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debtId, "debt_pay"), null);
                }
            }
            case "ret_full" -> {
                Long debtId = Long.parseLong(parts[2]);
                var debtOpt = debtService.getDebt(debtId, user.getId());
                if (debtOpt.isPresent()) {
                    Debt debt = debtOpt.get();
                    debtFlowService.startSession(user.getId(), DebtFlowService.FlowType.RETURN_LENT, debtId, debt.getPersonName(), debt.getRemainingAmount());
                    var session = debtFlowService.getSession(user.getId());
                    session.setPaymentAmount(debt.getRemainingAmount());
                    session.setFull(true);
                    apiClient.editMessageText(chatId, messageId, "Pul qayerga tushdi?",
                            inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debtId, "debt_ret"), null);
                }
            }
            case "cancel" -> apiClient.editMessageText(chatId, messageId, "❌ Bekor qilindi.", null, null);
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
            String msg = reportService.formatDailyReport(data, user.getId());
            apiClient.editMessageText(chatId, messageId, msg, inlineKeyboardFactory.getDailyReportActionsKeyboard(today), "HTML");
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
                    if (saved.type() == TransactionType.EXPENSE) {
                        dailyProfitService.deductFromProfit(user, today, saved.amount());
                    }
                    DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

                    String successMsg = BotMessageBuilder.buildSaveSuccessMessage(
                            saved, stats.totalExpense(), stats.totalIncome(), stats.netProfit());

                    apiClient.editMessageText(chatId, messageId, successMsg,
                            inlineKeyboardFactory.getSavedTransactionKeyboard(saved.id()), "HTML");
                    userService.updateState(user.getTelegramId(), UserState.IDLE);

                } catch (ValidationException e) {
                    apiClient.sendMessage(chatId, "⚠️ " + e.getMessage(), replyKeyboardFactory.getMainMenu(), null);
                } catch (Exception e) {
                    log.error("Failed to save draft {}: {}", draftId, e.getMessage(), e);
                    apiClient.sendMessage(chatId, "⚠️ Xatolik yuz berdi. Qaytadan urinib ko‘ring.",
                            replyKeyboardFactory.getMainMenu(), null);
                }
            }
            case "save_yesterday" -> handleSaveExpense(user, chatId, messageId, draftId, true);
            case "save_today" -> handleSaveExpense(user, chatId, messageId, draftId, false);
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
                        inlineKeyboardFactory.getDraftConfirmationKeyboard(draftId, dto.type()), "HTML");
            }
            case "back" -> {
                TransactionDraft d = draftService.getDraft(draftId, user.getId());
                DraftDto dto = draftService.toDto(d);
                String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone());
                apiClient.editMessageText(chatId, messageId, msg,
                        inlineKeyboardFactory.getDraftConfirmationKeyboard(draftId, dto.type()), "HTML");
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

    private void handleSaveExpense(User user, Long chatId, Integer messageId, Long draftId, boolean isYesterday) {
        String actionKey = "save_draft_" + draftId;
        if (!idempotencyService.tryAcquireAction(actionKey)) {
            log.warn("Duplicate save ignored for draft {}", draftId);
            return;
        }

        try {
            LocalDate today = DateTimeUtils.today(user.getTimezone());
            LocalDate targetDate = isYesterday ? today.minusDays(1) : today;

            TransactionDto saved = transactionService.confirmAndSaveWithDate(draftId, user.getId(), targetDate);

            // Deduct from profit for the target date
            dailyProfitService.deductFromProfit(user, targetDate, saved.amount());

            // Recalculate daily stats for targetDate
            DailyStatisticsDto targetStats = statisticsService.getDailyStatistics(user, targetDate);

            // If yesterday, re-save the closed summary for yesterday with updated numbers
            if (isYesterday) {
                dailySummaryService.closeDay(user, targetDate, targetStats.totalIncome(), targetStats.totalExpense(), targetStats.netProfit());
            }

            String successMsg = BotMessageBuilder.buildSaveSuccessMessageWithSource(
                    saved, isYesterday, targetStats.totalExpense(), targetStats.totalIncome(), targetStats.netProfit());

            apiClient.editMessageText(chatId, messageId, successMsg,
                    inlineKeyboardFactory.getSavedTransactionKeyboard(saved.id()), "HTML");
            userService.updateState(user.getTelegramId(), UserState.IDLE);

        } catch (ValidationException e) {
            apiClient.sendMessage(chatId, "⚠️ " + e.getMessage(), replyKeyboardFactory.getMainMenu(), null);
        } catch (Exception e) {
            log.error("Failed to save expense draft {} with isYesterday={}: {}", draftId, isYesterday, e.getMessage(), e);
            apiClient.sendMessage(chatId, "⚠️ Xatolik yuz berdi. Qaytadan urinib ko‘ring.",
                    replyKeyboardFactory.getMainMenu(), null);
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
                    var summary = dailySummaryService.closeDay(user, today, stats.totalIncome(), stats.totalExpense(), stats.netProfit());
                    String msg = String.format("""
                            🔐 <b>Kun yakunlandi va yopildi!</b>
                            ━━━━━━━━━━━━━━━━━━
                            📅 Sana: <b>%s</b>

                            💰 <b>Umumiy ishlab topilgan:</b>
                            <b>%s</b>

                            💸 <b>Xarajatlar:</b>
                            <b>%s</b>

                            ━━━━━━━━━━━━━━━━━━
                            ✅ <b>BUGUNGI FOYDANGIZ:</b>
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

        if ("list_today".equals(action) || "list_date".equals(action)) {
            LocalDate date = ("list_date".equals(action) && parts.length > 2)
                    ? LocalDate.parse(parts[2])
                    : DateTimeUtils.today(user.getTimezone());
            List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateOrderByCreatedAtAsc(user.getId(), date);
            if (transactions.isEmpty()) {
                apiClient.editMessageText(chatId, messageId,
                        "ℹ️ <b>" + DateTimeUtils.formatDate(date) + "</b> kuni operatsiyalar mavjud emas.",
                        inlineKeyboardFactory.getDailyReportActionsKeyboard(date), "HTML");
                return;
            }
            String msg = "🧾 <b>" + DateTimeUtils.formatDate(date) + " kungi operatsiyalar:</b>\n" +
                    "<i>Tahrirlash yoki kategoriyani o‘zgartirish uchun kerakli operatsiyani bosing:</i>";
            apiClient.editMessageText(chatId, messageId, msg,
                    inlineKeyboardFactory.getDailyOperationsKeyboard(transactions, date), "HTML");
            return;
        }

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
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                apiClient.editMessageText(chatId, messageId, "🗑 <b>Operatsiya o‘chirildi.</b>",
                        inlineKeyboardFactory.getDailyReportActionsKeyboard(today), "HTML");
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
            case "edit_field" -> {
                String field = parts[3];
                switch (field) {
                    case "cat" -> {
                        Transaction tx = transactionService.getByIdAndUser(txId, user.getId());
                        List<Category> categories = categoryService.getCategories(user.getId(), tx.getType());
                        apiClient.editMessageText(chatId, messageId, "📂 <b>Yangi kategoriyani tanlang:</b>",
                                inlineKeyboardFactory.getTransactionCategorySelectionKeyboard(txId, categories), "HTML");
                    }
                    case "amount" -> {
                        textMessageHandler.setUserEditingTransactionId(user.getId(), txId);
                        userService.updateState(user.getTelegramId(), UserState.WAITING_TX_EDIT_AMOUNT);
                        apiClient.sendMessage(chatId, "Yangi summani kiriting (masalan: <code>20000</code>):",
                                replyKeyboardFactory.getCancelMenu(), "HTML");
                    }
                    case "desc" -> {
                        textMessageHandler.setUserEditingTransactionId(user.getId(), txId);
                        userService.updateState(user.getTelegramId(), UserState.WAITING_TX_EDIT_DESCRIPTION);
                        apiClient.sendMessage(chatId, "Yangi izohni kiriting:", replyKeyboardFactory.getCancelMenu(), null);
                    }
                }
            }
            case "set_cat" -> {
                Long categoryId = Long.parseLong(parts[3]);
                Category cat = categoryService.getById(categoryId, user.getId());
                TransactionDto updated = transactionService.updateTransaction(txId, user.getId(), null, cat, null, null);
                String msg = "✅ <b>Kategoriya o‘zgartirildi!</b>\n\n" + BotMessageBuilder.buildTransactionDetail(updated, user.getTimezone());
                apiClient.editMessageText(chatId, messageId, msg,
                        inlineKeyboardFactory.getHistoryItemActionsKeyboard(txId), "HTML");
            }
        }
    }
}
