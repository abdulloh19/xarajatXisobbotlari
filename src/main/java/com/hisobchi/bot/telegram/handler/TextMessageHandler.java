package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.ai.dto.ParsedDebt;
import com.hisobchi.bot.ai.dto.ParsedTransaction;
import com.hisobchi.bot.ai.service.CategoryMatcher;
import com.hisobchi.bot.ai.service.DebtNlpService;
import com.hisobchi.bot.ai.service.TransactionNlpService;
import com.hisobchi.bot.ai.service.UzbekAmountParser;
import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.entity.CategoryType;
import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.common.util.UzbekDateParser;
import com.hisobchi.bot.debt.dto.DebtStatisticsDto;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtDraft;
import com.hisobchi.bot.debt.entity.DebtPayment;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtPaymentRepository;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.debt.service.DebtDraftService;
import com.hisobchi.bot.debt.service.DebtFlowService;
import com.hisobchi.bot.debt.service.DebtService;
import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.service.NotificationSettingsService;
import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.report.dto.ReportData;
import com.hisobchi.bot.report.service.ReportService;
import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.dto.MonthlyStatisticsDto;
import com.hisobchi.bot.statistics.dto.WeeklyStatisticsDto;
import com.hisobchi.bot.statistics.service.StatisticsService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextMessageHandler {

    private final TelegramApiClient apiClient;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TransactionDraftService draftService;
    private final StatisticsService statisticsService;
    private final UzbekAmountParser amountParser;
    private final UzbekDateParser dateParser;
    private final TransactionNlpService nlpService;
    private final DebtNlpService debtNlpService;
    private final DebtService debtService;
    private final DebtDraftService debtDraftService;
    private final DailyProfitService dailyProfitService;
    private final ReportService reportService;
    private final NotificationSettingsService notificationSettingsService;
    private final TransactionRepository transactionRepository;
    private final DebtRepository debtRepository;
    private final DebtPaymentRepository debtPaymentRepository;
    private final ReplyKeyboardFactory replyKeyboardFactory;
    private final InlineKeyboardFactory inlineKeyboardFactory;
    private final com.hisobchi.bot.debt.service.DebtFlowService debtFlowService;
    private final DebtNlpHandler debtNlpHandler;
    private final TransactionService transactionService;
    private final CategoryMatcher categoryMatcher;
    private final com.hisobchi.bot.todo.handler.TodoMessageHandler todoMessageHandler;

    // Temporary multi-step state storage per user
    private final ConcurrentHashMap<Long, BigDecimal> userCashProfits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BigDecimal> userDebtAmounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userDebtPersons = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDate> userDebtDates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, DebtType> userDebtTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> userActiveDebtDraftId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> userExtendingDebtId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> userEditingTransactionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDate> userProfitTargetDates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userActiveMenu = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> userDraftAddingCatId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> userTxAddingCatId = new ConcurrentHashMap<>();

    public void setUserDraftAddingCatId(Long userId, Long draftId) {
        userDraftAddingCatId.put(userId, draftId);
    }

    public void setUserTxAddingCatId(Long userId, Long txId) {
        userTxAddingCatId.put(userId, txId);
    }

    public void setProfitTargetDate(Long userId, LocalDate date) {
        if (date != null) {
            userProfitTargetDates.put(userId, date);
        } else {
            userProfitTargetDates.remove(userId);
        }
    }

    public LocalDate getProfitTargetDate(Long userId) {
        return userProfitTargetDates.get(userId);
    }

    public LocalDate removeProfitTargetDate(Long userId) {
        return userProfitTargetDates.remove(userId);
    }

    public void setUserActiveMenu(Long userId, String menu) {
        if (menu != null) {
            userActiveMenu.put(userId, menu);
        } else {
            userActiveMenu.remove(userId);
        }
    }

    public String getUserActiveMenu(Long userId) {
        return userActiveMenu.get(userId);
    }

    public void removeUserActiveMenu(Long userId) {
        userActiveMenu.remove(userId);
    }

    public void setUserEditingTransactionId(Long userId, Long txId) {
        userEditingTransactionId.put(userId, txId);
    }

    public void setExtendingDebtId(Long userId, Long debtId) {
        userExtendingDebtId.put(userId, debtId);
    }

    public BigDecimal removeUserDebtAmount(Long userId) { return userDebtAmounts.remove(userId); }
    public String removeUserDebtPerson(Long userId) { return userDebtPersons.remove(userId); }
    public LocalDate removeUserDebtDate(Long userId) { return userDebtDates.remove(userId); }
    public DebtType removeUserDebtType(Long userId) { return userDebtTypes.remove(userId); }
    public void clearUserDebtCreation(Long userId) {
        userDebtAmounts.remove(userId);
        userDebtPersons.remove(userId);
        userDebtDates.remove(userId);
        userDebtTypes.remove(userId);
    }

    public void clearUserTransientState(Long userId) {
        clearUserDebtCreation(userId);
        userExtendingDebtId.remove(userId);
        userEditingTransactionId.remove(userId);
        userDraftAddingCatId.remove(userId);
        userTxAddingCatId.remove(userId);
        userProfitTargetDates.remove(userId);
        userCashProfits.remove(userId);
    }

    private boolean isMainMenuOrSubmenuButton(String text) {
        if (text == null) return false;
        return switch (text) {
            case "💸 Xarajat qo‘shish", "💸 Xarajat qo'shish", "Xarajat qo‘shish", "Xarajat qo'shish",
                 "💰 Daromad qo‘shish", "💰 Daromad qo'shish", "Daromad qo‘shish", "Daromad qo'shish",
                 "🎙 Ovoz bilan kiritish",
                 "📊 Bugungi statistika", "📊 Statistika", "Statistika",
                 "📅 Haftalik statistika",
                 "🗓 Oylik statistika",
                 "📜 Tarix", "Tarix",
                 "📂 Kategoriyalar", "⚙️ Sozlamalar", "Sozlamalar",
                 "🔐 Kunni yopish", "Kunni yopish",
                 "💵 Foydani kiritish", "💵 Foyda kiritish", "Foydani kiritish", "Foyda kiritish", "💵 Foyda", "Foyda",
                 "🤝 Qarzlar", "📋 Qarzlar", "Qarzlar",
                 "💰 Qarz oldim", "💵 Qarz oldim",
                 "💸 Qarz berdim", "💰 Qarz berdim",
                 "💳 Qarz to‘lash", "💳 Qarz to'lash", "Qarz to‘lash", "Qarz to'lash",
                 "💵 Qarz qaytardi", "Qarz qaytardi",
                 "📋 Men olgan qarzlar", "Mening qarzlarim",
                 "📋 Men bergan qarzlar", "Men bergan qarzlar",
                 "⚠️ Muddati yaqin",
                 "📋 Faol qarzlar",
                 "✅ Yopilgan qarzlar",
                 "📊 Qarz statistikasi",
                 "📊 Hisobotlar", "Hisobotlar",
                 "📜 Bugun", "📜 Kecha", "📜 Oxirgi 7 kun", "📜 Shu oy",
                 "📅 Bugun", "Bugun",
                 "📅 Kecha", "Kecha",
                 "📅 Oxirgi 7 kun", "📆 Oxirgi 7 kun", "Oxirgi 7 kun",
                 "📆 Oxirgi 14 kun", "📅 Oxirgi 14 kun", "Oxirgi 14 kun",
                 "📆 Oxirgi 21 kun", "📅 Oxirgi 21 kun", "Oxirgi 21 kun",
                 "🗓 O‘tgan oy", "🗓 O'tgan oy", "📅 O‘tgan oy", "📅 O'tgan oy", "O‘tgan oy", "O'tgan oy",
                 "🗓 Shu oy", "📅 Shu oy", "Shu oy",
                 "✅ Vazifalar", "Vazifalar", "📁 Loyihalar", "Loyihalar",
                 "⬅️ Orqaga", "⬅️ Asosiy menyu", "🏠 Asosiy menyu" -> true;
            default -> false;
        };
    }

    public void handle(User user, com.hisobchi.bot.telegram.client.model.TelegramModels.Message message) {
        String text = message.getText();
        if (text == null || text.isBlank()) return;

        Long chatId = message.getChat().getId();
        String trimmed = text.trim();

        // 1. Check Global Cancel / Navigation commands
        if (isCancelCommand(trimmed)) {
            userService.updateState(user.getTelegramId(), UserState.IDLE);
            clearUserTransientState(user.getId());
            userActiveMenu.remove(user.getId());
            apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
            return;
        }

        // 1.1 If user clicked any menu button, break out of any waiting state
        if (isMainMenuOrSubmenuButton(trimmed)) {
            if (user.getState() != null && user.getState() != UserState.IDLE) {
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                clearUserTransientState(user.getId());
            }
        } else if (user.getState() != null && user.getState() != UserState.IDLE) {
            // 2. Route based on User State (if in an interactive input flow)
            handleStateInput(user, chatId, trimmed);
            return;
        }

        // 3. Handle Main Menu and Submenu button clicks
        switch (trimmed) {
            case "💸 Xarajat qo‘shish", "💸 Xarajat qo'shish", "Xarajat qo‘shish", "Xarajat qo'shish" -> {
                userService.updateState(user.getTelegramId(), UserState.WAITING_EXPENSE_AMOUNT);
                String msg = """
                        💵 <b>Xarajat miqdorini kiriting:</b>

                        Masalan:
                        <code>15000</code>
                        yoki
                        <code>15 000</code>
                        yoki
                        <code>15 ming</code>
                        """;
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getCancelMenu(), "HTML");
                return;
            }
            case "💰 Daromad qo‘shish", "💰 Daromad qo'shish", "Daromad qo‘shish", "Daromad qo'shish" -> {
                userService.updateState(user.getTelegramId(), UserState.WAITING_INCOME_AMOUNT);
                String msg = """
                        💰 <b>Bugungi daromad summasini kiriting:</b>

                        Masalan:
                        <code>2500000</code>
                        yoki
                        <code>2.5 million</code>
                        """;
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getCancelMenu(), "HTML");
                return;
            }
            case "🎙 Ovoz bilan kiritish" -> {
                String msg = """
                        🎙 <b>Ovozli xabar orqali kiritish:</b>

                        Mikrofon tugmasini bosib ushlab turing va ayting:
                        Masalan:
                        <i>"15 ming obedga"</i>
                        <i>"300 ming benzin oldim"</i>
                        <i>"Montajdan 2 million ishladim"</i>
                        <i>"Rustam akadan 2.5 million qarz oldim, 10 chigacha"</i>
                        """;
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getMainMenu(), "HTML");
                return;
            }
            case "📊 Bugungi statistika", "📊 Statistika", "Statistika" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);
                String msg = BotMessageBuilder.buildDailyStatisticsMessage(stats);
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDailyReportActionsKeyboard(today), "HTML");
                return;
            }
            case "📅 Haftalik statistika" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                WeeklyStatisticsDto stats = statisticsService.getWeeklyStatistics(user, today);
                String msg = BotMessageBuilder.buildWeeklyStatisticsMessage(stats);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getMainMenu(), "HTML");
                return;
            }
            case "🗓 Oylik statistika" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                MonthlyStatisticsDto stats = statisticsService.getMonthlyStatistics(user, today);
                String msg = BotMessageBuilder.buildMonthlyStatisticsMessage(stats);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getMainMenu(), "HTML");
                return;
            }
            case "📜 Tarix", "Tarix" -> {
                setUserActiveMenu(user.getId(), "HISTORY");
                apiClient.sendMessage(chatId, "📜 <b>Tarix bo‘limi:</b>\nDavrni tanlang:",
                        replyKeyboardFactory.getHistoryMenu(), "HTML");
                return;
            }
            case "📂 Kategoriyalar", "⚙️ Sozlamalar", "Sozlamalar" -> {
                apiClient.sendMessage(chatId, "⚙️ <b>Sozlamalar va Kategoriyalar:</b>",
                        replyKeyboardFactory.getSettingsMenu(), "HTML");
                return;
            }
            case "🔐 Kunni yopish", "Kunni yopish" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

                String msg = String.format("""
                        🔐 <b>Kunni yopmoqchimisiz?</b>

                        💰 <b>Umumiy ishlab topilgan:</b>
                        <b>%s</b>

                        💸 <b>Xarajatlar:</b>
                        <b>%s</b>

                        ━━━━━━━━━━━━━━━━━━
                        ✅ <b>Bugungi foydangiz:</b>
                        <b>%s</b>
                        ━━━━━━━━━━━━━━━━━━
                        """,
                        MoneyFormatter.format(stats.totalIncome()),
                        MoneyFormatter.format(stats.totalExpense()),
                        MoneyFormatter.format(stats.netProfit())
                );
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getCloseDayConfirmationKeyboard(), "HTML");
                return;
            }
            case "💵 Foydani kiritish", "💵 Foyda kiritish", "Foydani kiritish", "Foyda kiritish", "💵 Foyda", "Foyda", "foyda", "bugungi foyda", "kunlik foyda", "kunlik foydani kiritish" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                initiateProfitFlow(user, chatId, today);
                return;
            }
            case "🤝 Qarzlar", "📋 Qarzlar" -> {
                BigDecimal borrowed = debtService.getTotalActiveAmount(user.getId(), DebtType.BORROWED);
                BigDecimal lent = debtService.getTotalActiveAmount(user.getId(), DebtType.LENT);
                String overview = String.format("""
                        🤝 <b>QARZLAR BO‘LIMI</b>
                        ━━━━━━━━━━━━━━━━━━

                        🔴 <b>Olingan qarzlar:</b> %s
                        🟢 <b>Berilgan qarzlar:</b> %s

                        Quyidagi bo‘limlardan birini tanlang:
                        """,
                        MoneyFormatter.format(borrowed),
                        MoneyFormatter.format(lent)
                );
                apiClient.sendMessage(chatId, overview, replyKeyboardFactory.getDebtsMenu(), "HTML");
                return;
            }
            case "💰 Qarz oldim", "💵 Qarz oldim" -> {
                userDebtTypes.put(user.getId(), DebtType.BORROWED);
                userService.updateState(user.getTelegramId(), UserState.WAITING_BORROW_PERSON);
                apiClient.sendMessage(chatId,
                        "👤 <b>Kimdan qarz oldingiz?</b>\n\nMasalan:\n<i>Rustam aka</i>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
                return;
            }
            case "💸 Qarz berdim", "💰 Qarz berdim" -> {
                userDebtTypes.put(user.getId(), DebtType.LENT);
                userService.updateState(user.getTelegramId(), UserState.WAITING_LENT_PERSON);
                apiClient.sendMessage(chatId,
                        "👤 <b>Kimga qarz berdingiz?</b>\n\nMasalan:\n<i>Rustam aka</i>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
                return;
            }
            case "💳 Qarz to‘lash", "💳 Qarz to'lash", "Qarz to‘lash", "Qarz to'lash" -> {
                List<Debt> borrowed = debtService.getActiveDebts(user.getId(), DebtType.BORROWED);
                if (borrowed.isEmpty()) {
                    apiClient.sendMessage(chatId, "✅ Sizda to‘lanishi kerak bo‘lgan qarzlar yo‘q.", replyKeyboardFactory.getDebtsMenu(), null);
                    return;
                }
                var grouped = borrowed.stream().collect(java.util.stream.Collectors.groupingBy(Debt::getPersonName));
                apiClient.sendMessage(chatId, "Kimga qarz to‘lamoqchisiz?",
                        inlineKeyboardFactory.getDebtGroupedSelectionKeyboard(grouped, "debt_pay"), null);
                return;
            }
            case "💵 Qarz qaytardi", "Qarz qaytardi" -> {
                List<Debt> lent = debtService.getActiveDebts(user.getId(), DebtType.LENT);
                if (lent.isEmpty()) {
                    apiClient.sendMessage(chatId, "✅ Siz bergan faol qarzlar mavjud emas.", replyKeyboardFactory.getDebtsMenu(), null);
                    return;
                }
                var grouped = lent.stream().collect(java.util.stream.Collectors.groupingBy(Debt::getPersonName));
                apiClient.sendMessage(chatId, "Kim qarz qaytardi?",
                        inlineKeyboardFactory.getDebtGroupedSelectionKeyboard(grouped, "debt_ret"), null);
                return;
            }
            case "📋 Men olgan qarzlar", "Mening qarzlarim", "mening qarzlarim" -> {
                List<Debt> borrowed = debtService.getActiveDebts(user.getId(), DebtType.BORROWED);
                String msg = BotMessageBuilder.buildActiveDebtsList(borrowed, DebtType.BORROWED);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getDebtsMenu(), "HTML");
                return;
            }
            case "📋 Men bergan qarzlar", "Men bergan qarzlar" -> {
                List<Debt> lent = debtService.getActiveDebts(user.getId(), DebtType.LENT);
                String msg = BotMessageBuilder.buildActiveDebtsList(lent, DebtType.LENT);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getDebtsMenu(), "HTML");
                return;
            }
            case "⚠️ Muddati yaqin" -> {
                List<Debt> nearDue = debtService.getNearDueDebts(user.getId(), 3);
                String msg = BotMessageBuilder.buildNearDueDebtsMessage(nearDue);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getDebtsMenu(), "HTML");
                return;
            }
            case "📋 Faol qarzlar" -> {
                List<Debt> borrowed = debtService.getActiveDebts(user.getId(), DebtType.BORROWED);
                List<Debt> lent = debtService.getActiveDebts(user.getId(), DebtType.LENT);
                String msg = BotMessageBuilder.buildActiveDebtsOverviewMessage(borrowed, lent);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getDebtsMenu(), "HTML");
                return;
            }
            case "✅ Yopilgan qarzlar" -> {
                List<Debt> closed = debtService.getClosedDebts(user.getId());
                String msg = BotMessageBuilder.buildClosedDebtsMessage(closed);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getDebtsMenu(), "HTML");
                return;
            }
            case "📊 Qarz statistikasi" -> {
                DebtStatisticsDto stats = debtService.getDebtStatistics(user.getId());
                String msg = BotMessageBuilder.buildDebtStatisticsMessage(stats);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getDebtsMenu(), "HTML");
                return;
            }
            case "📊 Hisobotlar", "Hisobotlar" -> {
                setUserActiveMenu(user.getId(), "REPORTS");
                apiClient.sendMessage(chatId, "📊 <b>Davriy hisobotlar:</b>\nKerakli davrni tanlang:",
                        replyKeyboardFactory.getReportsMenu(), "HTML");
                return;
            }
            case "📜 Bugun" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                sendDayHistory(chatId, user, today, "Bugungi");
                return;
            }
            case "📜 Kecha" -> {
                LocalDate yesterday = DateTimeUtils.today(user.getTimezone()).minusDays(1);
                sendDayHistory(chatId, user, yesterday, "Kechagi");
                return;
            }
            case "📜 Oxirgi 7 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(6);
                sendPeriodHistory(chatId, user, start, end, "Oxirgi 7 kunlik");
                return;
            }
            case "📜 Shu oy" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                LocalDate start = today.withDayOfMonth(1);
                sendPeriodHistory(chatId, user, start, today, "Shu oylik");
                return;
            }
            case "📅 Kecha", "Kecha" -> {
                LocalDate yesterday = DateTimeUtils.today(user.getTimezone()).minusDays(1);
                sendDayHistory(chatId, user, yesterday, "Kechagi");
                return;
            }
            case "📅 Oxirgi 7 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(6);
                sendPeriodHistory(chatId, user, start, end, "Oxirgi 7 kunlik");
                return;
            }
            case "📆 Oxirgi 7 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(6);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "HAFTALIK HISOBOT");
                sendPeriodReport(chatId, data);
                return;
            }
            case "Oxirgi 7 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(6);
                if ("HISTORY".equals(userActiveMenu.get(user.getId()))) {
                    sendPeriodHistory(chatId, user, start, end, "Oxirgi 7 kunlik");
                } else {
                    ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "HAFTALIK HISOBOT");
                    sendPeriodReport(chatId, data);
                }
                return;
            }
            case "📆 Oxirgi 14 kun", "📅 Oxirgi 14 kun", "Oxirgi 14 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(13);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "2 HAFTALIK HISOBOT");
                sendPeriodReport(chatId, data);
                return;
            }
            case "📆 Oxirgi 21 kun", "📅 Oxirgi 21 kun", "Oxirgi 21 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(20);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "3 HAFTALIK HISOBOT");
                sendPeriodReport(chatId, data);
                return;
            }
            case "🗓 O‘tgan oy", "🗓 O'tgan oy", "📅 O‘tgan oy", "📅 O'tgan oy", "O‘tgan oy", "O'tgan oy" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                LocalDate prev = today.minusMonths(1);
                LocalDate start = prev.withDayOfMonth(1);
                LocalDate end = prev.with(TemporalAdjusters.lastDayOfMonth());
                String title = DateTimeUtils.formatUzbekMonthYear(prev) + " HISOBOTI";
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, title);
                sendPeriodReport(chatId, data);
                return;
            }
            case "🗓 Shu oy", "📅 Shu oy", "Shu oy" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                LocalDate start = today.withDayOfMonth(1);
                if ("HISTORY".equals(userActiveMenu.get(user.getId()))) {
                    sendPeriodHistory(chatId, user, start, today, "Shu oylik");
                } else {
                    String title = DateTimeUtils.formatUzbekMonthYear(today) + " HISOBOTI";
                    ReportData data = reportService.getPeriodReportData(user.getId(), start, today, title);
                    sendPeriodReport(chatId, data);
                }
                return;
            }
            case "📅 Bugun", "Bugun" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                if ("HISTORY".equals(userActiveMenu.get(user.getId()))) {
                    sendDayHistory(chatId, user, today, "Bugungi");
                } else {
                    ReportData report = reportService.getDailyReportData(user.getId(), today);
                    String msg = reportService.formatDailyReport(report, user.getId());
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDailyReportActionsKeyboard(today), "HTML");
                }
                return;
            }
            case "🔔 Eslatmalar", "Eslatmalar" -> {
                NotificationSettings s = notificationSettingsService.getOrCreateSettings(user);
                String msg = "🔔 <b>Avtomatik eslatmalar va hisobotlar sozlamalari:</b>\n\nKerakli bandni yoqish yoki o‘chirish uchun ustiga bosing:";
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getNotificationSettingsKeyboard(s), "HTML");
                return;
            }
            case "➕ Yangi kategoriya", "Yangi kategoriya" -> {
                userService.updateState(user.getTelegramId(), UserState.WAITING_NEW_CATEGORY_NAME);
                apiClient.sendMessage(chatId, "Yangi kategoriya nomini kiriting (masalan: <i>Kutubxona</i>):",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
                return;
            }
            case "🔄 Ma'lumotlarni tozalash", "🔄 Bugundan boshlash (tozalash)", "/reset", "/tozalash", "tozalash" -> {
                apiClient.sendMessage(chatId,
                        """
                        ⚙️ <b>Ma'lumotlarni tozalash bo‘limi:</b>

                        Qaysi ma'lumotlarni o‘chirmoqchisiz? Quyidagilardan birini tanlang:
                        """,
                        inlineKeyboardFactory.getDataResetMenuKeyboard(), "HTML");
                return;
            }
            case "foydalarni ochirish", "foydani ochirish", "foydani butkul ochirish" -> {
                apiClient.sendMessage(chatId,
                        """
                        ⚠️ <b>Foyda va daromadlarni tozalash:</b>

                        Shu paytgacha kiritilgan <b>barcha kunlik foydalar va daromadlar</b> butkul o‘chiriladi va nollashtiriladi.
                        <i>Xarajatlaringiz va qarzlaringiz o‘chirilmaydi.</i>

                        Rostdan ham barcha foyda va daromadlarni o‘chirmoqchimisiz?
                        """,
                        inlineKeyboardFactory.getResetProfitConfirmKeyboard(), "HTML");
                return;
            }
            case "xarajatlarni ochirish", "xarajatni ochirish", "xarajatni butkul ochirish" -> {
                apiClient.sendMessage(chatId,
                        """
                        ⚠️ <b>Xarajatlarni tozalash:</b>

                        Shu paytgacha kiritilgan <b>barcha xarajat operatsiyalari</b> butkul o‘chiriladi.
                        <i>Foyda va daromadlaringiz o‘chirilmaydi.</i>

                        Rostdan ham barcha xarajatlarni o‘chirmoqchimisiz?
                        """,
                        inlineKeyboardFactory.getResetExpenseConfirmKeyboard(), "HTML");
                return;
            }
        }

        // 3.5 Check To-Do commands or natural task message
        if (todoMessageHandler.handleCommandOrButton(user, chatId, trimmed)) {
            return;
        }

        // 4. Natural Language Text Processing (Debt or Transaction)
        handleNaturalTextMessage(user, chatId, trimmed);
    }

    private void handleStateInput(User user, Long chatId, String text) {
        if (todoMessageHandler.handleState(user, chatId, text)) {
            return;
        }

        UserState state = user.getState();

        switch (state) {
            case WAITING_WORK_DAY_CONFIRMATION -> {
                LocalDate targetDate = getProfitTargetDate(user.getId());
                if (targetDate == null) {
                    targetDate = DateTimeUtils.today(user.getTimezone());
                }
                String dateLabel = targetDate.equals(DateTimeUtils.today(user.getTimezone()))
                        ? "Bugungi"
                        : DateTimeUtils.formatUzbekDate(targetDate);

                String lower = text.trim().toLowerCase();
                if (lower.contains("ha") || lower.contains("ishla") || lower.contains("yes") || lower.equals("+")) {
                    setProfitTargetDate(user.getId(), targetDate);
                    userService.updateState(user.getTelegramId(), UserState.WAITING_DAILY_PROFIT_CASH);
                    apiClient.sendMessage(chatId,
                            "💵 <b>" + dateLabel + " naqd puldagi foydani kiriting:</b>\n<i>Masalan: 120000 yoki 120 ming</i>\n(Agar naqd bo'lmasa <code>0</code> deb yozing)",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                } else if (lower.contains("yo'q") || lower.contains("yoq") || lower.contains("dam") || lower.contains("no") || lower.equals("-")) {
                    dailyProfitService.markOffDay(user, targetDate);
                    removeProfitTargetDate(user.getId());
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId,
                            "🏖 <b>" + dateLabel + " dam!</b>\n\nMaroqli dam oling! Ushbu kunda qilingan xarajatlar oldingi ishlagan kuningiz foydasidan hisoblanadi.",
                            replyKeyboardFactory.getMainMenu(), "HTML");
                } else {
                    apiClient.sendMessage(chatId,
                            "💼 <b>Iltimos, tanlang: " + dateLabel + " ishladingizmi?</b>",
                            inlineKeyboardFactory.getWorkDayConfirmationKeyboard(targetDate), "HTML");
                }
            }
            case WAITING_DAILY_PROFIT_CASH -> {
                BigDecimal cash = BigDecimal.ZERO;
                if (!"0".equals(text.trim())) {
                    Optional<BigDecimal> amt = amountParser.parse(text);
                    if (amt.isPresent() && amt.get().compareTo(BigDecimal.ZERO) >= 0) {
                        cash = amt.get();
                    } else {
                        apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (yoki <code>0</code> deb yozing):",
                                replyKeyboardFactory.getCancelMenu(), "HTML");
                        return;
                    }
                }
                userCashProfits.put(user.getId(), cash);
                LocalDate targetDate = getProfitTargetDate(user.getId());
                if (targetDate == null) {
                    targetDate = DateTimeUtils.today(user.getTimezone());
                }
                String dateLabel = targetDate.equals(DateTimeUtils.today(user.getTimezone()))
                        ? "Bugungi"
                        : DateTimeUtils.formatUzbekDate(targetDate);
                userService.updateState(user.getTelegramId(), UserState.WAITING_DAILY_PROFIT_CARD);
                apiClient.sendMessage(chatId,
                        "💳 <b>" + dateLabel + " kartadagi foydani kiriting:</b>\n<i>Masalan: 80000 yoki 80 ming</i>\n(Agar kartada bo'lmasa <code>0</code> deb yozing)",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case WAITING_DAILY_PROFIT_CARD -> {
                BigDecimal card = BigDecimal.ZERO;
                if (!"0".equals(text.trim())) {
                    Optional<BigDecimal> amt = amountParser.parse(text);
                    if (amt.isPresent() && amt.get().compareTo(BigDecimal.ZERO) >= 0) {
                        card = amt.get();
                    } else {
                        apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (yoki <code>0</code> deb yozing):",
                                replyKeyboardFactory.getCancelMenu(), "HTML");
                        return;
                    }
                }

                BigDecimal cash = userCashProfits.remove(user.getId());
                if (cash == null) cash = BigDecimal.ZERO;

                LocalDate targetDate = removeProfitTargetDate(user.getId());
                if (targetDate == null) {
                    targetDate = DateTimeUtils.today(user.getTimezone());
                }
                dailyProfitService.saveOrUpdateProfit(user, targetDate, cash, card);

                BigDecimal totalProfit = cash.add(card);
                BigDecimal expense = transactionRepository.sumAmountByUserIdAndTypeAndDate(
                        user.getId(), TransactionType.EXPENSE, targetDate);
                if (expense == null) expense = BigDecimal.ZERO;
                BigDecimal totalEarned = expense.add(totalProfit);

                String dateLabel = targetDate.equals(DateTimeUtils.today(user.getTimezone()))
                        ? "Bugungi"
                        : DateTimeUtils.formatUzbekDate(targetDate);

                String msg = String.format("""
                        ✅ <b>%s foyda saqlandi!</b>

                        💵 <b>Naqd:</b> %s
                        💳 <b>Karta:</b> %s
                        ━━━━━━━━━━━━━━━━━━
                        ✅ <b>FOYDA:</b> %s
                        💸 <b>Xarajatlar:</b> %s
                        💰 <b>Umumiy ishlab topilgan:</b> %s
                        ━━━━━━━━━━━━━━━━━━
                        """,
                        dateLabel,
                        MoneyFormatter.format(cash),
                        MoneyFormatter.format(card),
                        MoneyFormatter.format(totalProfit),
                        MoneyFormatter.format(expense),
                        MoneyFormatter.format(totalEarned)
                );

                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getMainMenuReturnKeyboard(), "HTML");
            }
            case WAITING_PROFIT_ADD_AMOUNT -> {
                Optional<BigDecimal> amt = amountParser.parse(text);
                if (amt.isEmpty() || amt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ Iltimos, to‘g‘ri summa kiriting (masalan: <code>100000</code> yoki <code>100 ming</code>):",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                BigDecimal addAmount = amt.get();
                LocalDate targetDate = removeProfitTargetDate(user.getId());
                if (targetDate == null) {
                    targetDate = DateTimeUtils.today(user.getTimezone());
                }
                DailyProfit oldProfit = dailyProfitService.getProfit(user.getId(), targetDate).orElse(null);
                BigDecimal oldTotal = (oldProfit != null && oldProfit.getTotalProfit() != null) ? oldProfit.getTotalProfit() : BigDecimal.ZERO;

                DailyProfit updated = dailyProfitService.addToProfit(user, targetDate, addAmount);
                userService.updateState(user.getTelegramId(), UserState.IDLE);

                String dateLabel = targetDate.equals(DateTimeUtils.today(user.getTimezone())) ? "Bugungi" : DateTimeUtils.formatUzbekDate(targetDate);
                String msg = String.format("""
                        ✅ <b>%s foydaga summa qo‘shildi!</b>

                        📌 Oldingi foyda: <b>%s</b>
                        ➕ Qo‘shildi: <b>%s</b>
                        ━━━━━━━━━━━━━━━━━━
                        ✅ <b>Yangi foyda:</b> <b>%s</b>
                        <i>(💵 Naqd: %s | 💳 Karta: %s)</i>
                        """,
                        dateLabel,
                        MoneyFormatter.format(oldTotal),
                        MoneyFormatter.format(addAmount),
                        MoneyFormatter.format(updated != null ? updated.getTotalProfit() : oldTotal.add(addAmount)),
                        MoneyFormatter.format(updated != null ? updated.getCashAmount() : BigDecimal.ZERO),
                        MoneyFormatter.format(updated != null ? updated.getCardAmount() : BigDecimal.ZERO)
                );
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getMainMenuReturnKeyboard(), "HTML");
            }
            case WAITING_PROFIT_SUBTRACT_AMOUNT -> {
                Optional<BigDecimal> amt = amountParser.parse(text);
                if (amt.isEmpty() || amt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ Iltimos, to‘g‘ri summa kiriting (masalan: <code>100000</code> yoki <code>100 ming</code>):",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                BigDecimal subAmount = amt.get();
                LocalDate targetDate = removeProfitTargetDate(user.getId());
                if (targetDate == null) {
                    targetDate = DateTimeUtils.today(user.getTimezone());
                }
                DailyProfit oldProfit = dailyProfitService.getProfit(user.getId(), targetDate).orElse(null);
                BigDecimal oldTotal = (oldProfit != null && oldProfit.getTotalProfit() != null) ? oldProfit.getTotalProfit() : BigDecimal.ZERO;

                DailyProfit updated = dailyProfitService.subtractFromProfit(user, targetDate, subAmount);
                userService.updateState(user.getTelegramId(), UserState.IDLE);

                String dateLabel = targetDate.equals(DateTimeUtils.today(user.getTimezone())) ? "Bugungi" : DateTimeUtils.formatUzbekDate(targetDate);
                String msg = String.format("""
                        ✅ <b>%s foydadan summa ayirildi (minus qilindi)!</b>

                        📌 Oldingi foyda: <b>%s</b>
                        ➖ Ayirildi: <b>%s</b>
                        ━━━━━━━━━━━━━━━━━━
                        ✅ <b>Yangi foyda:</b> <b>%s</b>
                        <i>(💵 Naqd: %s | 💳 Karta: %s)</i>
                        """,
                        dateLabel,
                        MoneyFormatter.format(oldTotal),
                        MoneyFormatter.format(subAmount),
                        MoneyFormatter.format(updated != null ? updated.getTotalProfit() : oldTotal.subtract(subAmount).max(BigDecimal.ZERO)),
                        MoneyFormatter.format(updated != null ? updated.getCashAmount() : BigDecimal.ZERO),
                        MoneyFormatter.format(updated != null ? updated.getCardAmount() : BigDecimal.ZERO)
                );
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getMainMenuReturnKeyboard(), "HTML");
            }
            case WAITING_LENT_PERSON -> {
                userDebtPersons.put(user.getId(), text.trim());
                userDebtTypes.put(user.getId(), DebtType.LENT);
                userService.updateState(user.getTelegramId(), UserState.WAITING_LENT_AMOUNT);
                apiClient.sendMessage(chatId, "💰 <b>Qancha berdingiz?</b>\n\nMasalan:\n<code>2500000</code>", replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case WAITING_LENT_AMOUNT -> {
                Optional<BigDecimal> amtOpt = amountParser.parse(text);
                if (amtOpt.isEmpty() || amtOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>2500000</code>):", replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                userDebtAmounts.put(user.getId(), amtOpt.get());
                userService.updateState(user.getTelegramId(), UserState.WAITING_LENT_DATE);
                apiClient.sendMessage(chatId, "📅 <b>Qachongacha qaytarishi kerak?</b>\n\nMasalan:\n<code>10 oktabr</code>\n<code>keyingi juma</code>\n<code>ertaga</code>\n\n(yoki <i>'O‘tkazib yuborish'</i> deb yozing)", replyKeyboardFactory.getSkipOrCancelMenu(), "HTML");
            }
            case WAITING_LENT_DATE -> {
                LocalDate dueDate = null;
                if (!"⏭ O‘tkazib yuborish".equalsIgnoreCase(text) && !"o‘tkazib yuborish".equalsIgnoreCase(text)) {
                    ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
                    dueDate = dateParser.parseDate(text, zoneId).orElse(null);
                }
                userDebtDates.put(user.getId(), dueDate);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, "Pulni qanday berdingiz?", inlineKeyboardFactory.getDebtCreationPaymentMethodKeyboard(), null);
            }
            case WAITING_BORROW_PERSON -> {
                userDebtPersons.put(user.getId(), text.trim());
                userDebtTypes.put(user.getId(), DebtType.BORROWED);
                userService.updateState(user.getTelegramId(), UserState.WAITING_BORROW_AMOUNT);
                apiClient.sendMessage(chatId, "💰 <b>Qancha oldingiz?</b>\n\nMasalan:\n<code>2500000</code>", replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case WAITING_BORROW_AMOUNT -> {
                Optional<BigDecimal> amtOpt = amountParser.parse(text);
                if (amtOpt.isEmpty() || amtOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>2500000</code>):", replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                userDebtAmounts.put(user.getId(), amtOpt.get());
                userService.updateState(user.getTelegramId(), UserState.WAITING_BORROW_DATE);
                apiClient.sendMessage(chatId, "📅 <b>Qachon qaytarishingiz kerak?</b>\n\nMasalan:\n<code>10 oktabr</code>\n<code>keyingi haftaga</code>\n<code>ertaga</code>\n\n(yoki <i>'O‘tkazib yuborish'</i> deb yozing)", replyKeyboardFactory.getSkipOrCancelMenu(), "HTML");
            }
            case WAITING_BORROW_DATE -> {
                LocalDate dueDate = null;
                if (!"⏭ O‘tkazib yuborish".equalsIgnoreCase(text) && !"o‘tkazib yuborish".equalsIgnoreCase(text)) {
                    ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
                    dueDate = dateParser.parseDate(text, zoneId).orElse(null);
                }
                userDebtDates.put(user.getId(), dueDate);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, "Pulni qanday oldingiz?", inlineKeyboardFactory.getDebtCreationPaymentMethodKeyboard(), null);
            }
            case WAITING_DEBT_PAY_AMOUNT -> {
                var session = debtFlowService.getSession(user.getId());
                if (session == null || session.getDebtId() == null) {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
                    return;
                }
                Optional<BigDecimal> amtOpt = amountParser.parse(text);
                if (amtOpt.isEmpty() || amtOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>500000</code>):", replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                BigDecimal amt = amtOpt.get();
                var debtOpt = debtService.getDebt(session.getDebtId(), user.getId());
                if (debtOpt.isEmpty()) {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Qarz topilmadi.", replyKeyboardFactory.getMainMenu(), null);
                    return;
                }
                Debt debt = debtOpt.get();
                if (amt.compareTo(debt.getRemainingAmount()) > 0) {
                    String warnMsg = String.format("""
                            ⚠️ <b>%sga qolgan qarzingiz:</b>
                            %s

                            Siz:
                            <b>%s</b>

                            kiritdingiz.

                            Qolgan qarzdan ko‘p summa kiritib bo‘lmaydi.
                            """,
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount()),
                            MoneyFormatter.format(amt)
                    );
                    apiClient.sendMessage(chatId, warnMsg,
                            inlineKeyboardFactory.getOverpaymentBlockKeyboard(debt.getId(), debt.getRemainingAmount(), "debt_pay", true),
                            "HTML");
                    return;
                }
                session.setPaymentAmount(amt);
                session.setFull(amt.compareTo(debt.getRemainingAmount()) == 0);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, "Pul qayerdan beriladi?",
                        inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debt.getId(), "debt_pay"), null);
            }
            case WAITING_DEBT_RETURN_AMOUNT -> {
                var session = debtFlowService.getSession(user.getId());
                if (session == null || session.getDebtId() == null) {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
                    return;
                }
                Optional<BigDecimal> amtOpt = amountParser.parse(text);
                if (amtOpt.isEmpty() || amtOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>600000</code>):", replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                BigDecimal amt = amtOpt.get();
                var debtOpt = debtService.getDebt(session.getDebtId(), user.getId());
                if (debtOpt.isEmpty()) {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Qarz topilmadi.", replyKeyboardFactory.getMainMenu(), null);
                    return;
                }
                Debt debt = debtOpt.get();
                if (amt.compareTo(debt.getRemainingAmount()) > 0) {
                    String warnMsg = String.format("""
                            ⚠️ <b>%sning qolgan qarzi:</b>
                            %s

                            Siz:
                            <b>%s</b>

                            kiritdingiz.

                            Qolgan qarzdan ko‘p summa kiritib bo‘lmaydi.
                            """,
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount()),
                            MoneyFormatter.format(amt)
                    );
                    apiClient.sendMessage(chatId, warnMsg,
                            inlineKeyboardFactory.getOverpaymentBlockKeyboard(debt.getId(), debt.getRemainingAmount(), "debt_ret", false),
                            "HTML");
                    return;
                }
                session.setPaymentAmount(amt);
                session.setFull(amt.compareTo(debt.getRemainingAmount()) == 0);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, "Pulni qanday qaytardi?",
                        inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debt.getId(), "debt_ret"), null);
            }
            case WAITING_VOICE_DEBT_PERSON -> {
                var session = debtFlowService.getSession(user.getId());
                DebtType type = debtFlowService.getDebtType(user.getId());
                BigDecimal amount = debtFlowService.getDebtAmount(user.getId());
                LocalDate dueDate = null;

                if (session != null) {
                    if (type == null) {
                        type = session.getFlowType() == DebtFlowService.FlowType.RETURN_LENT ? DebtType.LENT : DebtType.BORROWED;
                    }
                    if (amount == null) {
                        amount = session.getPaymentAmount();
                    }
                    dueDate = session.getDueDate();
                }
                if (type == null) {
                    type = userDebtTypes.getOrDefault(user.getId(), DebtType.BORROWED);
                }
                if (amount == null) {
                    amount = userDebtAmounts.getOrDefault(user.getId(), BigDecimal.ZERO);
                }

                String cleanedPerson = com.hisobchi.bot.ai.service.DebtNlpService.cleanEntityName(text.trim());
                if (cleanedPerson.isBlank()) cleanedPerson = text.trim();

                debtFlowService.clear(user.getId());
                userDebtAmounts.remove(user.getId());
                userDebtTypes.remove(user.getId());
                userService.updateState(user.getTelegramId(), UserState.IDLE);

                DebtDraft draft = debtDraftService.createDraft(
                        user, type, amount, cleanedPerson, dueDate, null, text, 0.9, "Naqd", LocalDate.now()
                );
                String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(draft);
                apiClient.sendMessage(chatId, confirmMsg,
                        inlineKeyboardFactory.getDebtConfirmationKeyboard(draft.getId(), draft.getType()), "HTML");
                apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
            }
            case WAITING_VOICE_DEBT_DATE -> {
                var session = debtFlowService.getSession(user.getId());
                if (session != null) {
                    LocalDate dueDate = null;
                    if (!"⏭ O‘tkazib yuborish".equalsIgnoreCase(text) && !"o‘tkazib yuborish".equalsIgnoreCase(text)) {
                        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
                        dueDate = dateParser.parseDate(text, zoneId).orElse(null);
                    }
                    DebtType type = session.getFlowType() == DebtFlowService.FlowType.RETURN_LENT ? DebtType.LENT : DebtType.BORROWED;
                    userDebtTypes.put(user.getId(), type);
                    userDebtAmounts.put(user.getId(), session.getPaymentAmount());
                    userDebtPersons.put(user.getId(), session.getPersonName());
                    userDebtDates.put(user.getId(), dueDate);
                    debtFlowService.clearSession(user.getId());
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Pulni qanday " + (type == DebtType.BORROWED ? "oldingiz?" : "berdingiz?"), inlineKeyboardFactory.getDebtCreationPaymentMethodKeyboard(), null);
                } else {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
                }
            }
            case WAITING_DEBT_AMOUNT -> {
                Optional<BigDecimal> amountOpt = amountParser.parse(text);
                if (amountOpt.isEmpty() || amountOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>1500000</code> yoki <code>1.5 million</code>):",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                userDebtAmounts.put(user.getId(), amountOpt.get());
                String existingPerson = debtFlowService.getDebtPerson(user.getId());
                if (existingPerson == null) existingPerson = userDebtPersons.get(user.getId());

                DebtType type = userDebtTypes.getOrDefault(user.getId(), DebtType.BORROWED);
                if (existingPerson != null && !existingPerson.isBlank() && !"Noma'lum".equalsIgnoreCase(existingPerson)) {
                    userDebtPersons.put(user.getId(), existingPerson);
                    debtFlowService.clear(user.getId());
                    DebtDraft draft = debtDraftService.createDraft(
                            user, type, amountOpt.get(), existingPerson, null, null, text, 0.9, "Naqd", LocalDate.now()
                    );
                    String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(draft);
                    apiClient.sendMessage(chatId, confirmMsg,
                            inlineKeyboardFactory.getDebtConfirmationKeyboard(draft.getId(), draft.getType()), "HTML");
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
                    return;
                }

                userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_PERSON);
                String personPrompt = type == DebtType.BORROWED
                        ? "👤 <b>Kimdan yoki nimadan oldingiz?</b>\n\nMasalan:\n<i>Moy</i>, <i>Zapravka</i>, <i>Akmal</i>"
                        : "👤 <b>Kimga berdingiz?</b>\n\nMasalan:\n<i>Javlon</i>";
                apiClient.sendMessage(chatId, personPrompt, replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case WAITING_DEBT_PERSON -> {
                String cleanedPerson = com.hisobchi.bot.ai.service.DebtNlpService.cleanEntityName(text.trim());
                userDebtPersons.put(user.getId(), cleanedPerson.isBlank() ? text.trim() : cleanedPerson);
                DebtType type = userDebtTypes.getOrDefault(user.getId(), DebtType.BORROWED);
                userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_DATE);
                String datePrompt = type == DebtType.BORROWED
                        ? "📅 <b>Qachon qaytarishingiz kerak?</b>\n\nMasalan:\n<code>10 oktabr</code>\n<code>15.10.2026</code>\n<code>2 haftadan keyin</code>\n<code>ertaga</code>\n<code>25 kun ichida</code>\n\n(yoki <i>'O‘tkazib yuborish'</i> deb yozing)"
                        : "📅 <b>Qachon qaytarishi kerak?</b>\n\nMasalan:\n<code>20 oktabr</code>\n<code>15.10.2026</code>\n<code>2 haftadan keyin</code>\n<code>ertaga</code>\n\n(yoki <i>'O‘tkazib yuborish'</i> deb yozing)";
                apiClient.sendMessage(chatId, datePrompt, replyKeyboardFactory.getSkipOrCancelMenu(), "HTML");
            }
            case WAITING_DEBT_DATE -> {
                LocalDate dueDate = null;
                if (!"⏭ O‘tkazib yuborish".equalsIgnoreCase(text) && !"o‘tkazib yuborish".equalsIgnoreCase(text)) {
                    ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
                    Optional<LocalDate> dOpt = dateParser.parseDate(text, zoneId);
                    if (dOpt.isPresent()) {
                        dueDate = dOpt.get();
                    }
                }
                userDebtDates.put(user.getId(), dueDate != null ? dueDate : LocalDate.now().plusWeeks(1));
                userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_PAYMENT_METHOD);
                apiClient.sendMessage(chatId, "💳 <b>To‘lov turini tanlang:</b>",
                        replyKeyboardFactory.getPaymentMethodMenu(), "HTML");
            }
            case WAITING_DEBT_PAYMENT_METHOD -> {
                String paymentMethod = text.toLowerCase().contains("karta") ? "Karta" : "Naqd";
                BigDecimal amount = userDebtAmounts.remove(user.getId());
                String person = userDebtPersons.remove(user.getId());
                LocalDate dueDate = userDebtDates.remove(user.getId());
                DebtType type = userDebtTypes.getOrDefault(user.getId(), DebtType.BORROWED);
                userDebtTypes.remove(user.getId());

                if (amount == null) amount = BigDecimal.ZERO;
                if (person == null) person = "Noma'lum";

                DebtDraft draft = debtDraftService.createDraft(
                        user, type, amount, person, dueDate, null, null, 1.0, paymentMethod, LocalDate.now()
                );

                String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(draft);
                apiClient.sendMessage(chatId, confirmMsg,
                        inlineKeyboardFactory.getDebtConfirmationKeyboard(draft.getId(), draft.getType()), "HTML");
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
            }
            case WAITING_DEBT_EXTEND_DATE -> {
                Long debtId = userExtendingDebtId.remove(user.getId());
                ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
                Optional<LocalDate> newDateOpt = dateParser.parseDate(text, zoneId);
                if (newDateOpt.isPresent() && debtId != null) {
                    LocalDate newDate = newDateOpt.get();
                    var debtOpt = debtService.getDebt(debtId, user.getId());
                    if (debtOpt.isPresent()) {
                        Debt d = debtOpt.get();
                        String oldDateStr = d.getDueDate() != null ? d.getDueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Belgilanmagan";
                        String newDateStr = newDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                        String confirmMsg = String.format("""
                                📅 <b>Yangi muddat</b>

                                Eski sana:
                                <b>%s</b>

                                Yangi sana:
                                <b>%s</b>

                                <b>Tasdiqlaysizmi?</b>
                                """, oldDateStr, newDateStr);
                        apiClient.sendMessage(chatId, confirmMsg,
                                inlineKeyboardFactory.getDebtDateExtensionConfirmKeyboard(debtId, newDate), "HTML");
                    }
                } else {
                    apiClient.sendMessage(chatId, "⚠️ Sanani aniqlab bo‘lmadi.", replyKeyboardFactory.getMainMenu(), null);
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_DEBT_EDIT_AMOUNT -> {
                Optional<BigDecimal> amountOpt = amountParser.parse(text);
                Long draftId = userActiveDebtDraftId.get(user.getId());
                if (amountOpt.isPresent() && draftId != null) {
                    debtDraftService.findValidDraft(draftId, user.getId()).ifPresent(d -> {
                        d.setAmount(amountOpt.get());
                        String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(d);
                        apiClient.sendMessage(chatId, confirmMsg,
                                inlineKeyboardFactory.getDebtConfirmationKeyboard(d.getId(), d.getType()), "HTML");
                    });
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_DEBT_EDIT_PERSON -> {
                Long draftId = userActiveDebtDraftId.get(user.getId());
                if (draftId != null) {
                    debtDraftService.findValidDraft(draftId, user.getId()).ifPresent(d -> {
                        d.setPersonName(text.trim());
                        String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(d);
                        apiClient.sendMessage(chatId, confirmMsg,
                                inlineKeyboardFactory.getDebtConfirmationKeyboard(d.getId(), d.getType()), "HTML");
                    });
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_DEBT_EDIT_DATE -> {
                Long draftId = userActiveDebtDraftId.get(user.getId());
                if (draftId != null) {
                    ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
                    LocalDate dDate = dateParser.parseDate(text, zoneId).orElse(null);
                    debtDraftService.findValidDraft(draftId, user.getId()).ifPresent(d -> {
                        d.setDueDate(dDate);
                        String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(d);
                        apiClient.sendMessage(chatId, confirmMsg,
                                inlineKeyboardFactory.getDebtConfirmationKeyboard(d.getId(), d.getType()), "HTML");
                    });
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_EXPENSE_AMOUNT -> {
                Optional<BigDecimal> amountOpt = amountParser.parse(text);
                if (amountOpt.isEmpty() || amountOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ Iltimos, to‘g‘ri summa kiriting (masalan: <code>15000</code> yoki <code>15 ming</code>):",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }

                BigDecimal amount = amountOpt.get();
                TransactionDraft draft = draftService.createDraft(
                        user, TransactionType.EXPENSE, amount, null, null, TransactionSource.MANUAL, text, 1.0);

                userService.updateState(user.getTelegramId(), UserState.WAITING_EXPENSE_CATEGORY);
                List<Category> categories = categoryService.getCategories(user.getId(), TransactionType.EXPENSE);

                String prompt = "💵 Summa: <b>" + MoneyFormatter.format(amount) + "</b>\n\n📂 <b>Kategoriyani tanlang yoki yangi nom yozing:</b>";
                apiClient.sendMessage(chatId, prompt,
                        inlineKeyboardFactory.getCategorySelectionKeyboard(draft.getId(), categories), "HTML");
            }
            case WAITING_EXPENSE_CATEGORY -> {
                Optional<TransactionDraft> draftOpt = draftService.getLatestPendingDraft(user.getId());
                if (draftOpt.isEmpty()) {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "⚠️ Xarajat ma'lumotlari topilmadi. Qaytadan kiriting.",
                            replyKeyboardFactory.getMainMenu(), null);
                    return;
                }

                TransactionDraft draft = draftOpt.get();
                Category cat = categoryService.getOrCreateCategory(user, text, "📌", TransactionType.EXPENSE);
                draft = draftService.updateDraftCategory(draft.getId(), user.getId(), cat);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                sendDraftConfirmation(user, chatId, draft);
            }
            case WAITING_INCOME_AMOUNT -> {
                Optional<BigDecimal> amountOpt = amountParser.parse(text);
                if (amountOpt.isEmpty() || amountOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ Iltimos, to‘g‘ri summa kiriting (masalan: <code>2500000</code>):",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }

                BigDecimal amount = amountOpt.get();
                TransactionDraft draft = draftService.createDraft(
                        user, TransactionType.INCOME, amount, null, null, TransactionSource.MANUAL, text, 1.0);

                userService.updateState(user.getTelegramId(), UserState.WAITING_INCOME_DESCRIPTION);
                List<Category> categories = categoryService.getCategories(user.getId(), TransactionType.INCOME);

                String prompt = "💰 Daromad summasi: <b>" + MoneyFormatter.format(amount) + "</b>\n\n" +
                        "Manbani tanlang yoki yangisini yozing (masalan: <i>\"Konditsioner montajidan\"</i>):";

                apiClient.sendMessage(chatId, prompt,
                        inlineKeyboardFactory.getCategorySelectionKeyboard(draft.getId(), categories), "HTML");
            }
            case WAITING_INCOME_DESCRIPTION -> {
                Optional<TransactionDraft> draftOpt = draftService.getLatestPendingDraft(user.getId());
                if (draftOpt.isEmpty()) {
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    apiClient.sendMessage(chatId, "Draft topilmadi. Qaytadan boshlang.", replyKeyboardFactory.getMainMenu(), null);
                    return;
                }

                TransactionDraft draft = draftOpt.get();
                if (!"⏭ O‘tkazib yuborish".equals(text)) {
                    Category cat = categoryService.getOrCreateCategory(user, text, "📌", TransactionType.INCOME);
                    draft = draftService.updateDraftCategory(draft.getId(), user.getId(), cat);
                    draft.setDescription(text);
                }

                sendDraftConfirmation(user, chatId, draft);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_AMOUNT_EDIT -> {
                Optional<BigDecimal> amountOpt = amountParser.parse(text);
                if (amountOpt.isEmpty() || amountOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting:", replyKeyboardFactory.getCancelMenu(), null);
                    return;
                }

                Optional<TransactionDraft> draftOpt = draftService.getLatestPendingDraft(user.getId());
                if (draftOpt.isPresent()) {
                    TransactionDraft draft = draftService.updateDraftAmount(draftOpt.get().getId(), user.getId(), amountOpt.get());
                    sendDraftConfirmation(user, chatId, draft);
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_DESCRIPTION_EDIT -> {
                Optional<TransactionDraft> draftOpt = draftService.getLatestPendingDraft(user.getId());
                if (draftOpt.isPresent()) {
                    TransactionDraft draft = draftService.updateDraftDescription(draftOpt.get().getId(), user.getId(), text);
                    sendDraftConfirmation(user, chatId, draft);
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_CATEGORY_EDIT -> {
                Long draftId = userDraftAddingCatId.remove(user.getId());
                Long txId = userTxAddingCatId.remove(user.getId());
                if (draftId != null) {
                    TransactionDraft draft = draftService.getDraft(draftId, user.getId());
                    Category cat = categoryService.getOrCreateCategory(user, text, "📌", draft.getType());
                    draft = draftService.updateDraftCategory(draftId, user.getId(), cat);
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    sendDraftConfirmation(user, chatId, draft);
                } else if (txId != null) {
                    Transaction tx = transactionService.getByIdAndUser(txId, user.getId());
                    Category cat = categoryService.getOrCreateCategory(user, text, "📌", tx.getType());
                    TransactionDto updated = transactionService.updateTransaction(txId, user.getId(), null, cat, null, null);
                    userService.updateState(user.getTelegramId(), UserState.IDLE);
                    String msg = "✅ <b>Yangi kategoriya biriktirildi!</b>\n\n" + BotMessageBuilder.buildTransactionDetail(updated, user.getTimezone());
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getHistoryItemActionsKeyboard(txId, updated.transactionDate()), "HTML");
                } else {
                    Optional<TransactionDraft> draftOpt = draftService.getLatestPendingDraft(user.getId());
                    if (draftOpt.isPresent()) {
                        TransactionDraft draft = draftOpt.get();
                        Category cat = categoryService.getOrCreateCategory(user, text, "📌", draft.getType());
                        draft = draftService.updateDraftCategory(draft.getId(), user.getId(), cat);
                        userService.updateState(user.getTelegramId(), UserState.IDLE);
                        sendDraftConfirmation(user, chatId, draft);
                    } else {
                        userService.updateState(user.getTelegramId(), UserState.IDLE);
                        apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
                    }
                }
            }
            case WAITING_TX_EDIT_AMOUNT -> {
                Optional<BigDecimal> amountOpt = amountParser.parse(text);
                if (amountOpt.isEmpty() || amountOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>20000</code>):",
                            replyKeyboardFactory.getCancelMenu(), null);
                    return;
                }
                Long txId = userEditingTransactionId.remove(user.getId());
                if (txId != null) {
                    TransactionDto updated = transactionService.updateTransaction(txId, user.getId(), amountOpt.get(), null, null, null);
                    String msg = "✅ <b>Summa yangilandi!</b>\n\n" + BotMessageBuilder.buildTransactionDetail(updated, user.getTimezone());
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getHistoryItemActionsKeyboard(txId), "HTML");
                } else {
                    apiClient.sendMessage(chatId, "Operatsiya topilmadi.", replyKeyboardFactory.getMainMenu(), null);
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_TX_EDIT_DESCRIPTION -> {
                Long txId = userEditingTransactionId.remove(user.getId());
                if (txId != null) {
                    String desc = ("❌ Bekor qilish".equals(text) || "⏭ O‘tkazib yuborish".equals(text)) ? null : text;
                    TransactionDto updated = transactionService.updateTransaction(txId, user.getId(), null, null, desc, null);
                    String msg = "✅ <b>Izoh yangilandi!</b>\n\n" + BotMessageBuilder.buildTransactionDetail(updated, user.getTimezone());
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getHistoryItemActionsKeyboard(txId), "HTML");
                } else {
                    apiClient.sendMessage(chatId, "Operatsiya topilmadi.", replyKeyboardFactory.getMainMenu(), null);
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_NEW_CATEGORY_NAME -> {
                Category cat = categoryService.createCustomCategory(user, text, "📌", CategoryType.EXPENSE);
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, "✅ Yangi kategoriya qo‘shildi: " + cat.getDisplayName(),
                        replyKeyboardFactory.getMainMenu(), null);
            }
            default -> {
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
            }
        }
    }

    private void handleNaturalTextMessage(User user, Long chatId, String text) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");

        // 1. Check if natural text is a debt message
        Optional<ParsedDebt> debtOpt = debtNlpService.parse(text, zoneId);
        if (debtOpt.isPresent()) {
            debtNlpHandler.handleParsedDebt(user, chatId, debtOpt.get(), text);
            return;
        }

        // Safety guard: NEVER treat a debt-related message as an expense/income transaction
        String lowerText = text.toLowerCase();
        if (lowerText.contains("qarz") || lowerText.contains("nasiya")) {
            apiClient.sendMessage(chatId,
                    "🤝 <b>Qarz ma'lumoti</b> deb tushundim, lekin to‘liq aniqlab bo‘lmadi.\n\n" +
                    "Iltimos, summani va kimdan/kimga ekanligini aniqroq yozing (masalan: <i>\"25 ming magazindan qarz\"</i> yoki <i>\"50 ming Aliga qarz berdim\"</i>):",
                    replyKeyboardFactory.getDebtsMenu(), "HTML");
            return;
        }

        // Safety guard: If message is asking for reports, history or duration periods, do NOT treat as expense/income
        if (lowerText.contains("oxirgi") || lowerText.contains("hisobot")
                || lowerText.contains("tarix") || lowerText.contains("statistika") || lowerText.contains("menyu")
                || lowerText.matches(".*\\b\\d+\\s*(?:kun|hafta|oy|yil)\\b.*")) {
            if (lowerText.contains("7 kun")) {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(6);
                if ("HISTORY".equals(userActiveMenu.get(user.getId()))) {
                    sendPeriodHistory(chatId, user, start, end, "Oxirgi 7 kunlik");
                } else {
                    ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "HAFTALIK HISOBOT");
                    sendPeriodReport(chatId, data);
                }
                return;
            } else if (lowerText.contains("14 kun")) {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(13);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "2 HAFTALIK HISOBOT");
                sendPeriodReport(chatId, data);
                return;
            } else if (lowerText.contains("21 kun")) {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(20);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "3 HAFTALIK HISOBOT");
                sendPeriodReport(chatId, data);
                return;
            } else if (lowerText.contains("kecha")) {
                LocalDate yesterday = DateTimeUtils.today(user.getTimezone()).minusDays(1);
                sendDayHistory(chatId, user, yesterday, "Kechagi");
                return;
            } else if (lowerText.contains("bugun") && lowerText.contains("tarix")) {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                sendDayHistory(chatId, user, today, "Bugungi");
                return;
            } else if (lowerText.contains("bugun") && lowerText.contains("hisobot")) {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                ReportData report = reportService.getDailyReportData(user.getId(), today);
                String msg = reportService.formatDailyReport(report, user.getId());
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDailyReportActionsKeyboard(today), "HTML");
                return;
            } else if (lowerText.contains("shu oy")) {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                LocalDate start = today.withDayOfMonth(1);
                String title = DateTimeUtils.formatUzbekMonthYear(today) + " HISOBOTI";
                ReportData data = reportService.getPeriodReportData(user.getId(), start, today, title);
                sendPeriodReport(chatId, data);
                return;
            } else if (lowerText.contains("tarix")) {
                setUserActiveMenu(user.getId(), "HISTORY");
                apiClient.sendMessage(chatId, "📜 <b>Tarix bo‘limi:</b>\nDavrni tanlang:",
                        replyKeyboardFactory.getHistoryMenu(), "HTML");
                return;
            } else if (lowerText.contains("hisobot")) {
                setUserActiveMenu(user.getId(), "REPORTS");
                apiClient.sendMessage(chatId, "📊 <b>Davriy hisobotlar:</b>\nKerakli davrni tanlang:",
                        replyKeyboardFactory.getReportsMenu(), "HTML");
                return;
            }
        }

        // Quick natural commands for profit adjustments and profit expenses
        if (handleQuickProfitCommand(user, chatId, text)) {
            return;
        }

        // 2. Otherwise parse as standard transaction (expense/income)
        Optional<ParsedTransaction> parsedOpt = nlpService.parse(text);
        if (parsedOpt.isPresent()) {
            ParsedTransaction parsed = parsedOpt.get();

            Category category = null;
            if (parsed.category() != null && !parsed.category().isBlank()) {
                category = categoryService.findByName(user.getId(), parsed.category(), parsed.type()).orElse(null);
            }

            TransactionDraft draft = draftService.createDraft(
                    user,
                    parsed.type(),
                    parsed.amount(),
                    category,
                    parsed.description(),
                    TransactionSource.TEXT,
                    text,
                    parsed.confidence()
            );

            if (category != null) {
                sendDraftConfirmation(user, chatId, draft);
            } else {
                List<Category> categories = categoryService.getCategories(user.getId(), parsed.type());
                String prompt = "✍️ <b>" + MoneyFormatter.format(parsed.amount()) + "</b> " +
                        (parsed.type() == TransactionType.INCOME ? "daromad" : "xarajat") +
                        " deb tushundim.\n\n<b>Qaysi kategoriyaga qo‘shay?</b>";
                apiClient.sendMessage(chatId, prompt,
                        inlineKeyboardFactory.getCategorySelectionKeyboard(draft.getId(), categories), "HTML");
            }
        } else {
            String unknown = """
                    ⚠️ Kechirasiz, xabarni tushunmadim.

                    Masalan quyidagicha yozishingiz yoki ovoz yuborishingiz mumkin:
                    ✍️ <i>"15 ming obed"</i>
                    ✍️ <i>"300 ming benzin"</i>
                    ✍️ <i>"Rustam akadan 2 million qarz oldim, keyingi haftagacha"</i>
                    """;
            apiClient.sendMessage(chatId, unknown, replyKeyboardFactory.getMainMenu(), "HTML");
        }
    }

    public void initiateProfitFlow(User user, Long chatId, LocalDate targetDate) {
        setProfitTargetDate(user.getId(), targetDate);
        Optional<DailyProfit> existingOpt = dailyProfitService.getProfit(user.getId(), targetDate);
        String dateLabel = targetDate.equals(DateTimeUtils.today(user.getTimezone()))
                ? "Bugungi"
                : DateTimeUtils.formatUzbekDate(targetDate);

        if (existingOpt.isPresent() && existingOpt.get().isWorkDay()
                && existingOpt.get().getTotalProfit() != null
                && existingOpt.get().getTotalProfit().compareTo(BigDecimal.ZERO) > 0) {
            DailyProfit existing = existingOpt.get();
            String msg = String.format("""
                    💰 <b>%s foyda:</b> <b>%s</b>
                    <i>(💵 Naqd: %s | 💳 Karta: %s)</i>

                    Foydani o‘zgartirish uchun amalni tanlang:
                    ➕ <b>Qo‘shish</b> — mavjud foydaga summa qo‘shish
                    ➖ <b>Minus qilish</b> — foydadan summa ayirish
                    ✏️ <b>Yangitdan kiritish</b> — yangi summa bilan to‘liq almashtirish
                    """,
                    dateLabel,
                    MoneyFormatter.format(existing.getTotalProfit()),
                    MoneyFormatter.format(existing.getCashAmount()),
                    MoneyFormatter.format(existing.getCardAmount())
            );
            apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getProfitEditOptionsKeyboard(targetDate), "HTML");
        } else {
            userService.updateState(user.getTelegramId(), UserState.WAITING_WORK_DAY_CONFIRMATION);
            apiClient.sendMessage(chatId,
                    "💼 <b>" + dateLabel + " ishladingizmi?</b>\n\nAgar dam olgan bo‘lsangiz, xarajatlar oldingi ishlagan kuningiz foydasidan hisoblanadi.",
                    inlineKeyboardFactory.getWorkDayConfirmationKeyboard(targetDate), "HTML");
        }
    }

    public void setActiveDebtDraftForEdit(Long userId, Long draftId) {
        userActiveDebtDraftId.put(userId, draftId);
    }

    public boolean handleQuickProfitCommand(User user, Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lowerText = text.toLowerCase().trim();

        if (!lowerText.contains("foyda")) {
            return false;
        }

        if (lowerText.equals("foyda") || lowerText.equals("bugungi foyda") || lowerText.equals("kunlik foyda")
                || lowerText.equals("foydani kiritish") || lowerText.equals("kunlik foydani kiritish")) {
            LocalDate today = DateTimeUtils.today(user.getTimezone());
            initiateProfitFlow(user, chatId, today);
            return true;
        }

        // Case 1: Expense deducted from today's profit (e.g. "bugungi foydadan 10 min xarajatga qoshib qoy", "foydadan 20 ming xarajat")
        if (lowerText.contains("xarajat") || lowerText.contains("qarajat")) {
            Optional<BigDecimal> amtOpt = amountParser.parse(text);
            if (amtOpt.isPresent() && amtOpt.get().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal amount = amtOpt.get();
                LocalDate today = DateTimeUtils.today(user.getTimezone());

                Category category = null;
                Optional<String> matchedCat = categoryMatcher.matchCategory(text, TransactionType.EXPENSE);
                if (matchedCat.isPresent()) {
                    category = categoryService.findByName(user.getId(), matchedCat.get(), TransactionType.EXPENSE).orElse(null);
                }
                if (category == null) {
                    List<Category> expCats = categoryService.getCategories(user.getId(), TransactionType.EXPENSE);
                    if (!expCats.isEmpty()) {
                        category = expCats.get(0);
                    }
                }

                // Deduct from today's profit
                dailyProfitService.deductFromProfit(user, today, amount);

                Transaction tx = Transaction.builder()
                        .user(user)
                        .amount(amount)
                        .type(TransactionType.EXPENSE)
                        .category(category)
                        .currency("UZS")
                        .description("Bugungi foydadan xarajat")
                        .source(TransactionSource.TEXT)
                        .transactionDate(today)
                        .build();
                Transaction saved = transactionRepository.save(tx);

                DailyProfit updatedProfit = dailyProfitService.getProfit(user.getId(), today).orElse(null);
                DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

                String catName = saved.getCategory() != null
                        ? (saved.getCategory().getEmoji() != null ? saved.getCategory().getEmoji() + " " : "") + saved.getCategory().getName()
                        : "Xarajat";

                BigDecimal todayProfitVal = (updatedProfit != null && updatedProfit.getTotalProfit() != null)
                        ? updatedProfit.getTotalProfit()
                        : dailyProfitService.getTodayProfitOrIncome(user, today);
                BigDecimal cashVal = updatedProfit != null ? updatedProfit.getCashAmount() : todayProfitVal;
                BigDecimal cardVal = updatedProfit != null ? updatedProfit.getCardAmount() : BigDecimal.ZERO;

                String msg = String.format("""
                        ✅ <b>Xarajat bugungi foydadan ayirildi va saqlandi!</b>

                        💸 <b>Xarajat:</b> <b>%s</b>
                        📂 <b>Kategoriya:</b> %s
                        ━━━━━━━━━━━━━━━━━━
                        💰 <b>Umumiy kunlik topilgan pul (o‘zgarmadi):</b> <b>%s</b>
                        💰 <b>Bugungi yangi foyda:</b> <b>%s</b>
                        <i>(💵 Naqd: %s | 💳 Karta: %s)</i>

                        💸 <b>Bugungi jami xarajat:</b> <b>%s</b>
                        """,
                        MoneyFormatter.format(saved.getAmount()),
                        catName,
                        MoneyFormatter.format(stats.totalIncome()),
                        MoneyFormatter.format(todayProfitVal),
                        MoneyFormatter.format(cashVal),
                        MoneyFormatter.format(cardVal),
                        MoneyFormatter.format(stats.totalExpense())
                );

                apiClient.sendMessage(chatId, msg,
                        inlineKeyboardFactory.getSavedTransactionKeyboard(saved.getId()), "HTML");
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
        }

        // Case 2: Minus from profit only (e.g. "foydadan 20 min minus", "foydadan 20 ming ayir", "foydadan 30000 olib tashla")
        if (lowerText.contains("minus") || lowerText.contains("ayir")
                || lowerText.contains("kamayt") || lowerText.contains("olib tashla") || lowerText.contains("olb tashla")) {
            Optional<BigDecimal> amtOpt = amountParser.parse(text);
            if (amtOpt.isPresent() && amtOpt.get().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal changeAmt = amtOpt.get();
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                DailyProfit oldProfit = dailyProfitService.getProfit(user.getId(), today).orElse(null);
                BigDecimal oldTotal = (oldProfit != null && oldProfit.getTotalProfit() != null) ? oldProfit.getTotalProfit() : BigDecimal.ZERO;

                DailyProfit updated = dailyProfitService.subtractFromProfit(user, today, changeAmt);
                String msg = String.format("""
                        ✅ <b>Bugungi foydadan ayirildi (minus qilindi)!</b>

                        📌 Oldingi foyda: <b>%s</b>
                        ➖ Ayirildi: <b>%s</b>
                        ━━━━━━━━━━━━━━━━━━
                        ✅ <b>Yangi foyda:</b> <b>%s</b>
                        <i>(💵 Naqd: %s | 💳 Karta: %s)</i>
                        """,
                        MoneyFormatter.format(oldTotal),
                        MoneyFormatter.format(changeAmt),
                        MoneyFormatter.format(updated != null ? updated.getTotalProfit() : oldTotal.subtract(changeAmt).max(BigDecimal.ZERO)),
                        MoneyFormatter.format(updated != null ? updated.getCashAmount() : BigDecimal.ZERO),
                        MoneyFormatter.format(updated != null ? updated.getCardAmount() : BigDecimal.ZERO)
                );
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getMainMenuReturnKeyboard(), "HTML");
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
        }

        // Case 3: Add to profit (e.g. "foydaga 20 min qo'sh", "bugungi foydaga 50 ming qo‘sh")
        if (lowerText.contains("qosh") || lowerText.contains("qo'sh") || lowerText.contains("qo‘sh")) {
            Optional<BigDecimal> amtOpt = amountParser.parse(text);
            if (amtOpt.isPresent() && amtOpt.get().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal changeAmt = amtOpt.get();
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                DailyProfit oldProfit = dailyProfitService.getProfit(user.getId(), today).orElse(null);
                BigDecimal oldTotal = (oldProfit != null && oldProfit.getTotalProfit() != null) ? oldProfit.getTotalProfit() : BigDecimal.ZERO;

                DailyProfit updated = dailyProfitService.addToProfit(user, today, changeAmt);
                String msg = String.format("""
                        ✅ <b>Bugungi foydaga qo‘shildi!</b>

                        📌 Oldingi foyda: <b>%s</b>
                        ➕ Qo‘shildi: <b>%s</b>
                        ━━━━━━━━━━━━━━━━━━
                        ✅ <b>Yangi foyda:</b> <b>%s</b>
                        <i>(💵 Naqd: %s | 💳 Karta: %s)</i>
                        """,
                        MoneyFormatter.format(oldTotal),
                        MoneyFormatter.format(changeAmt),
                        MoneyFormatter.format(updated != null ? updated.getTotalProfit() : oldTotal.add(changeAmt)),
                        MoneyFormatter.format(updated != null ? updated.getCashAmount() : BigDecimal.ZERO),
                        MoneyFormatter.format(updated != null ? updated.getCardAmount() : BigDecimal.ZERO)
                );
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getMainMenuReturnKeyboard(), "HTML");
                userService.updateState(user.getTelegramId(), UserState.IDLE);
                return true;
            }
        }

        return false;
    }

    private void sendDraftConfirmation(User user, Long chatId, TransactionDraft draft) {
        DraftDto dto = draftService.toDto(draft);
        LocalDate today = DateTimeUtils.today(user.getTimezone());
        boolean isOffDay = dailyProfitService.isOffDay(user.getId(), today);
        String lastWorkText = null;
        if (isOffDay) {
            lastWorkText = dailyProfitService.getLastWorkedDayProfit(user.getId(), today)
                    .map(p -> DateTimeUtils.formatUzbekDate(p.getProfitDate()))
                    .orElse("oldingi ishlagan kun");
        }
        boolean hasEnteredProfitToday = dailyProfitService.hasEnteredProfitToday(user, today);

        String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone(), isOffDay, lastWorkText, hasEnteredProfitToday);
        apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDraftConfirmationKeyboard(draft.getId(), draft.getType(), isOffDay, lastWorkText, hasEnteredProfitToday), "HTML");
    }

    private void sendDayHistory(Long chatId, User user, LocalDate date, String label) {
        List<Transaction> list = transactionRepository.findByUserIdAndTransactionDateOrderByCreatedAtAsc(user.getId(), date);
        List<Debt> debts = debtRepository != null
                ? debtRepository.findAllCreatedByUserIdAndDate(user.getId(), date)
                : List.of();
        List<DebtPayment> payments = debtPaymentRepository != null
                ? debtPaymentRepository.findByUserIdAndPaymentDateOrderByCreatedAtAsc(user.getId(), date)
                : List.of();
        DailyProfit profit = dailyProfitService.getProfit(user.getId(), date).orElse(null);

        String msg = BotMessageBuilder.buildDayTransactionsHistoryDetailed(date, list, debts, payments, profit, user.getTimezone());
        var markup = inlineKeyboardFactory.getDailyOperationsKeyboard(list, debts, date);
        apiClient.sendMessage(chatId, msg, markup, "HTML");
    }

    private void sendPeriodHistory(Long chatId, User user, LocalDate start, LocalDate end, String title) {
        List<Transaction> list = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                user.getId(), start, end);
        List<Debt> debts = debtRepository != null
                ? debtRepository.findAllCreatedByUserIdAndDateBetween(user.getId(), start, end)
                : List.of();
        List<DebtPayment> payments = debtPaymentRepository != null
                ? debtPaymentRepository.findByUserIdAndPaymentDateBetweenOrderByCreatedAtDesc(user.getId(), start, end)
                : List.of();
        List<DailyProfit> profits = dailyProfitService.getProfitsBetween(user.getId(), start, end);

        String msg = BotMessageBuilder.buildPeriodTransactionsHistory(title, start, end, list, debts, payments, profits, user.getTimezone());
        var markup = inlineKeyboardFactory.getTransactionsListKeyboard(list, debts, 15);
        apiClient.sendMessage(chatId, msg, markup, "HTML");
    }

    private void sendPeriodReport(Long chatId, ReportData data) {
        String msg = reportService.formatPeriodReport(data);
        var inlineKbd = inlineKeyboardFactory.getPeriodReportKeyboard(data.incompleteDates());
        if (inlineKbd != null) {
            apiClient.sendMessage(chatId, msg, inlineKbd, "HTML");
        } else {
            apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getReportsMenu(), "HTML");
        }
    }

    private boolean isCancelCommand(String text) {
        String lower = text.toLowerCase();
        return lower.equals("❌ bekor qilish") || lower.equals("bekor") ||
               lower.equals("cancel") || lower.equals("/cancel") ||
               lower.equals("⬅️ asosiy menyu");
    }
}
