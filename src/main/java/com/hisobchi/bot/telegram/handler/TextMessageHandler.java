package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.ai.dto.ParsedDebt;
import com.hisobchi.bot.ai.dto.ParsedTransaction;
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
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.service.DebtDraftService;
import com.hisobchi.bot.debt.service.DebtService;
import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.service.NotificationSettingsService;
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
import com.hisobchi.bot.transaction.entity.TransactionDraft;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.transaction.service.TransactionDraftService;
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
    private final ReplyKeyboardFactory replyKeyboardFactory;
    private final InlineKeyboardFactory inlineKeyboardFactory;

    // Temporary multi-step state storage per user
    private final ConcurrentHashMap<Long, BigDecimal> userCashProfits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BigDecimal> userDebtAmounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userDebtPersons = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDate> userDebtDates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, DebtType> userDebtTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> userActiveDebtDraftId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> userExtendingDebtId = new ConcurrentHashMap<>();

    public void setExtendingDebtId(Long userId, Long debtId) {
        userExtendingDebtId.put(userId, debtId);
    }

    public void handle(User user, com.hisobchi.bot.telegram.client.model.TelegramModels.Message message) {
        String text = message.getText();
        if (text == null || text.isBlank()) return;

        Long chatId = message.getChat().getId();
        String trimmed = text.trim();

        // 1. Check Global Cancel / Navigation commands
        if (isCancelCommand(trimmed)) {
            userService.updateState(user.getTelegramId(), UserState.IDLE);
            apiClient.sendMessage(chatId, "Asosiy menyu:", replyKeyboardFactory.getMainMenu(), null);
            return;
        }

        // 2. Route based on User State (if in an interactive input flow)
        if (user.getState() != null && user.getState() != UserState.IDLE) {
            handleStateInput(user, chatId, trimmed);
            return;
        }

        // 3. Handle Main Menu and Submenu button clicks
        switch (trimmed) {
            case "💸 Xarajat qo‘shish" -> {
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
            case "💰 Daromad qo‘shish" -> {
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
            case "📊 Bugungi statistika" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);
                String msg = BotMessageBuilder.buildDailyStatisticsMessage(stats);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getMainMenu(), "HTML");
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
            case "📜 Tarix" -> {
                apiClient.sendMessage(chatId, "📜 <b>Tarix bo‘limi:</b>\nDavrni tanlang:",
                        replyKeyboardFactory.getHistoryMenu(), "HTML");
                return;
            }
            case "📂 Kategoriyalar", "⚙️ Sozlamalar" -> {
                apiClient.sendMessage(chatId, "⚙️ <b>Sozlamalar va Kategoriyalar:</b>",
                        replyKeyboardFactory.getSettingsMenu(), "HTML");
                return;
            }
            case "🔐 Kunni yopish" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);

                String msg = String.format("""
                        🔐 <b>Kunni yopmoqchimisiz?</b>

                        💰 Daromad:
                        <b>%s</b>

                        💸 Xarajat:
                        <b>%s</b>

                        ✅ Sof foyda:
                        <b>%s</b>
                        """,
                        MoneyFormatter.format(stats.totalIncome()),
                        MoneyFormatter.format(stats.totalExpense()),
                        MoneyFormatter.format(stats.netProfit())
                );
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getCloseDayConfirmationKeyboard(), "HTML");
                return;
            }
            case "💵 Foydani kiritish" -> {
                userService.updateState(user.getTelegramId(), UserState.WAITING_DAILY_PROFIT_CASH);
                apiClient.sendMessage(chatId,
                        "💵 <b>Bugungi naqd puldagi foydani kiriting:</b>\n<i>Masalan: 120000 yoki 120 ming</i>\n(Agar naqd bo'lmasa <code>0</code> deb yozing)",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
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
            case "💵 Qarz oldim" -> {
                userDebtTypes.put(user.getId(), DebtType.BORROWED);
                userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_AMOUNT);
                apiClient.sendMessage(chatId,
                        "💰 <b>Qancha qarz oldingiz?</b>\n\nMasalan:\n<code>1500000</code>\nyoki\n<code>1 500 000</code>\nyoki\n<code>1.5 million</code>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
                return;
            }
            case "💰 Qarz berdim" -> {
                userDebtTypes.put(user.getId(), DebtType.LENT);
                userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_AMOUNT);
                apiClient.sendMessage(chatId,
                        "💰 <b>Qancha qarz berdingiz?</b>\n\nMasalan:\n<code>2000000</code>\nyoki\n<code>2 000 000</code>\nyoki\n<code>2 million</code>",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
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
            case "📊 Hisobotlar" -> {
                apiClient.sendMessage(chatId, "📊 <b>Davriy hisobotlar:</b>\nKerakli davrni tanlang:",
                        replyKeyboardFactory.getReportsMenu(), "HTML");
                return;
            }
            case "📆 Oxirgi 14 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(13);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "2 HAFTALIK HISOBOT");
                String msg = reportService.formatPeriodReport(data);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getReportsMenu(), "HTML");
                return;
            }
            case "📆 Oxirgi 21 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(20);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "3 HAFTALIK HISOBOT");
                String msg = reportService.formatPeriodReport(data);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getReportsMenu(), "HTML");
                return;
            }
            case "🗓 O‘tgan oy" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                LocalDate prev = today.minusMonths(1);
                LocalDate start = prev.withDayOfMonth(1);
                LocalDate end = prev.with(TemporalAdjusters.lastDayOfMonth());
                String title = prev.getMonth().name() + " " + prev.getYear() + " HISOBOTI";
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, title);
                String msg = reportService.formatPeriodReport(data);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getReportsMenu(), "HTML");
                return;
            }
            case "🔔 Eslatmalar" -> {
                NotificationSettings s = notificationSettingsService.getOrCreateSettings(user);
                String msg = "🔔 <b>Avtomatik eslatmalar va hisobotlar sozlamalari:</b>\n\nKerakli bandni yoqish yoki o‘chirish uchun ustiga bosing:";
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getNotificationSettingsKeyboard(s), "HTML");
                return;
            }
            case "➕ Yangi kategoriya" -> {
                userService.updateState(user.getTelegramId(), UserState.WAITING_NEW_CATEGORY_NAME);
                apiClient.sendMessage(chatId, "Yangi kategoriya nomini kiriting (masalan: <i>Kutubxona</i>):",
                        replyKeyboardFactory.getCancelMenu(), "HTML");
                return;
            }
            case "📅 Bugun" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                ReportData report = reportService.getDailyReportData(user.getId(), today);
                String msg = reportService.formatDailyReport(report);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getMainMenu(), "HTML");
                return;
            }
            case "📅 Kecha" -> {
                LocalDate yesterday = DateTimeUtils.today(user.getTimezone()).minusDays(1);
                ReportData report = reportService.getDailyReportData(user.getId(), yesterday);
                String msg = reportService.formatDailyReport(report);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getHistoryMenu(), "HTML");
                return;
            }
            case "📅 Oxirgi 7 kun" -> {
                LocalDate end = DateTimeUtils.today(user.getTimezone());
                LocalDate start = end.minusDays(6);
                ReportData data = reportService.getPeriodReportData(user.getId(), start, end, "HAFTALIK HISOBOT");
                String msg = reportService.formatPeriodReport(data);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getHistoryMenu(), "HTML");
                return;
            }
            case "🗓 Shu oy" -> {
                LocalDate today = DateTimeUtils.today(user.getTimezone());
                LocalDate start = today.withDayOfMonth(1);
                String title = today.getMonth().name() + " " + today.getYear() + " HISOBOTI";
                ReportData data = reportService.getPeriodReportData(user.getId(), start, today, title);
                String msg = reportService.formatPeriodReport(data);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getHistoryMenu(), "HTML");
                return;
            }
        }

        // 4. Natural Language Text Processing (Debt or Transaction)
        handleNaturalTextMessage(user, chatId, trimmed);
    }

    private void handleStateInput(User user, Long chatId, String text) {
        UserState state = user.getState();

        switch (state) {
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
                userService.updateState(user.getTelegramId(), UserState.WAITING_DAILY_PROFIT_CARD);
                apiClient.sendMessage(chatId,
                        "💳 <b>Bugungi kartadagi foydani kiriting:</b>\n<i>Masalan: 80000 yoki 80 ming</i>\n(Agar kartada bo'lmasa <code>0</code> deb yozing)",
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

                LocalDate today = DateTimeUtils.today(user.getTimezone());
                dailyProfitService.saveOrUpdateProfit(user, today, cash, card);

                BigDecimal totalProfit = cash.add(card);
                BigDecimal expense = transactionRepository.sumAmountByUserIdAndTypeAndDate(
                        user.getId(), TransactionType.EXPENSE, today);
                if (expense == null) expense = BigDecimal.ZERO;
                BigDecimal totalEarned = expense.add(totalProfit);

                String msg = String.format("""
                        ✅ <b>Bugungi foyda saqlandi!</b>

                        💵 <b>Naqd:</b> %s
                        💳 <b>Karta:</b> %s
                        💰 <b>Jami foyda:</b> %s

                        💸 <b>Bugungi xarajat:</b> %s
                        🚀 <b>Bugun ishladingiz:</b> %s
                        """,
                        MoneyFormatter.format(cash),
                        MoneyFormatter.format(card),
                        MoneyFormatter.format(totalProfit),
                        MoneyFormatter.format(expense),
                        MoneyFormatter.format(totalEarned)
                );

                userService.updateState(user.getTelegramId(), UserState.IDLE);
                apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getMainMenu(), "HTML");
            }
            case WAITING_DEBT_AMOUNT -> {
                Optional<BigDecimal> amountOpt = amountParser.parse(text);
                if (amountOpt.isEmpty() || amountOpt.get().compareTo(BigDecimal.ZERO) <= 0) {
                    apiClient.sendMessage(chatId, "⚠️ To‘g‘ri summa kiriting (masalan: <code>1500000</code> yoki <code>1.5 million</code>):",
                            replyKeyboardFactory.getCancelMenu(), "HTML");
                    return;
                }
                userDebtAmounts.put(user.getId(), amountOpt.get());
                DebtType type = userDebtTypes.getOrDefault(user.getId(), DebtType.BORROWED);
                userService.updateState(user.getTelegramId(), UserState.WAITING_DEBT_PERSON);
                String personPrompt = type == DebtType.BORROWED
                        ? "👤 <b>Kimdan oldingiz?</b>\n\nMasalan:\n<i>Akmal</i>"
                        : "👤 <b>Kimga berdingiz?</b>\n\nMasalan:\n<i>Javlon</i>";
                apiClient.sendMessage(chatId, personPrompt, replyKeyboardFactory.getCancelMenu(), "HTML");
            }
            case WAITING_DEBT_PERSON -> {
                userDebtPersons.put(user.getId(), text.trim());
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
                        inlineKeyboardFactory.getDebtConfirmationKeyboard(draft.getId()), "HTML");
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
                                inlineKeyboardFactory.getDebtConfirmationKeyboard(d.getId()), "HTML");
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
                                inlineKeyboardFactory.getDebtConfirmationKeyboard(d.getId()), "HTML");
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
                                inlineKeyboardFactory.getDebtConfirmationKeyboard(d.getId()), "HTML");
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

                String prompt = "💵 Summa: <b>" + MoneyFormatter.format(amount) + "</b>\n\n📂 <b>Kategoriyani tanlang:</b>";
                apiClient.sendMessage(chatId, prompt,
                        inlineKeyboardFactory.getCategorySelectionKeyboard(draft.getId(), categories), "HTML");
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
                        "Manbani tanlang yoki pastdan yozing (masalan: <i>\"Konditsioner montajidan\"</i>):";

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
                    draft.setDescription(text);
                }

                DraftDto dto = draftService.toDto(draft);
                String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone());
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDraftConfirmationKeyboard(draft.getId()), "HTML");
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
                    DraftDto dto = draftService.toDto(draft);
                    String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone());
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDraftConfirmationKeyboard(draft.getId()), "HTML");
                }
                userService.updateState(user.getTelegramId(), UserState.IDLE);
            }
            case WAITING_DESCRIPTION_EDIT -> {
                Optional<TransactionDraft> draftOpt = draftService.getLatestPendingDraft(user.getId());
                if (draftOpt.isPresent()) {
                    TransactionDraft draft = draftService.updateDraftDescription(draftOpt.get().getId(), user.getId(), text);
                    DraftDto dto = draftService.toDto(draft);
                    String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone());
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDraftConfirmationKeyboard(draft.getId()), "HTML");
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
            ParsedDebt d = debtOpt.get();
            DebtDraft draft = debtDraftService.createDraft(
                    user, d.type(), d.amount(), d.personName(), d.dueDate(), d.description(), text, d.confidence()
            );
            String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(draft);
            apiClient.sendMessage(chatId, confirmMsg,
                    inlineKeyboardFactory.getDebtConfirmationKeyboard(draft.getId()), "HTML");
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
                DraftDto dto = draftService.toDto(draft);
                String msg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone());
                apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDraftConfirmationKeyboard(draft.getId()), "HTML");
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

    public void setActiveDebtDraftForEdit(Long userId, Long draftId) {
        userActiveDebtDraftId.put(userId, draftId);
    }

    private boolean isCancelCommand(String text) {
        String lower = text.toLowerCase();
        return lower.equals("❌ bekor qilish") || lower.equals("bekor") ||
               lower.equals("cancel") || lower.equals("/cancel") ||
               lower.equals("⬅️ asosiy menyu");
    }
}
