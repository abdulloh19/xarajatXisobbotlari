package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.ai.dto.ParsedTransaction;
import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory;
import com.hisobchi.bot.transaction.dto.DraftDto;
import com.hisobchi.bot.transaction.entity.TransactionDraft;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.service.TransactionDraftService;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.voice.service.VoiceProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceMessageHandler {

    private final TelegramApiClient apiClient;
    private final VoiceProcessingService voiceProcessingService;
    private final TransactionDraftService draftService;
    private final CategoryService categoryService;
    private final InlineKeyboardFactory inlineKeyboardFactory;
    private final ReplyKeyboardFactory replyKeyboardFactory;
    private final com.hisobchi.bot.ai.service.DebtNlpService debtNlpService;
    private final com.hisobchi.bot.debt.service.DebtDraftService debtDraftService;

    private final com.hisobchi.bot.profit.service.DailyProfitService dailyProfitService;
    private final com.hisobchi.bot.telegram.handler.DebtNlpHandler debtNlpHandler;
    @org.springframework.context.annotation.Lazy
    private final TextMessageHandler textMessageHandler;
    private final com.hisobchi.bot.todo.handler.TodoMessageHandler todoMessageHandler;

    public void handle(User user, Message message) {
        Long chatId = message.getChat().getId();
        Voice voice = message.getVoice();

        if (voice == null || voice.getFileId() == null) {
            apiClient.sendMessage(chatId, "⚠️ Ovozli xabar aniqlanmadi.", replyKeyboardFactory.getMainMenu(), null);
            return;
        }

        apiClient.sendMessage(chatId, "🎙 <i>Ovozli xabar tinglanmoqda...</i>", null, "HTML");

        VoiceProcessingService.VoiceProcessResult result = voiceProcessingService.processVoice(voice.getFileId());

        if (!result.transcription().success() || result.transcription().text().isBlank()) {
            String errorMsg = result.transcription().errorMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = "Ovozni aniqlab bo‘lmadi. Iltimos, aniqroq gapirib qaytadan urinib ko‘ring.";
            }
            apiClient.sendMessage(chatId, "⚠️ " + errorMsg, replyKeyboardFactory.getMainMenu(), null);
            return;
        }

        String transcribedText = result.transcription().text();

        // 1. Check if voice message is a debt transaction (Voice Debt Parser)
        java.time.ZoneId zoneId = java.time.ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
        java.util.Optional<com.hisobchi.bot.ai.dto.ParsedDebt> debtOpt = debtNlpService.parse(transcribedText, zoneId);
        if (debtOpt.isPresent()) {
            debtNlpHandler.handleParsedDebt(user, chatId, debtOpt.get(), transcribedText);
            return;
        }

        // Safety guard: NEVER treat a debt-related utterance as an expense/income transaction
        String lowerVoice = transcribedText.toLowerCase();
        if (lowerVoice.contains("qarz") || lowerVoice.contains("nasiya")) {
            apiClient.sendMessage(chatId,
                    "🤝 <b>Qarz ma'lumoti</b> deb tushundim, lekin to‘liq aniqlab bo‘lmadi.\n\n" +
                    "Iltimos, summani va kimdan/kimga ekanligini aniqroq ayting (masalan: <i>\"25 ming magazindan qarz\"</i> yoki <i>\"50 ming Aliga qarz berdim\"</i>):",
                    replyKeyboardFactory.getDebtsMenu(), "HTML");
            return;
        }

        // Quick natural command for profit adjustments or profit expenses via voice
        if (textMessageHandler.handleQuickProfitCommand(user, chatId, transcribedText)) {
            return;
        }

        // Check if voice message is a task/reminder
        if (todoMessageHandler.handleCommandOrButton(user, chatId, transcribedText)) {
            return;
        }

        Optional<ParsedTransaction> parsedOpt = result.parsedTransaction();

        if (parsedOpt.isEmpty()) {
            apiClient.sendMessage(chatId,
                    "🎙 Siz aytdingiz:\n<i>\"" + BotMessageBuilder.escapeHtml(transcribedText) +
                    "\"</i>\n\n⚠️ Kechirasiz, bu xabardan summa yoki operatsiya turini aniqlay olmadim. Masalan: <i>\"15 ming obedga\"</i>",
                    replyKeyboardFactory.getMainMenu(), "HTML");
            return;
        }

        ParsedTransaction parsed = parsedOpt.get();

        // Check if category was recognized
        Category category = null;
        if (parsed.category() != null && !parsed.category().isBlank()) {
            category = categoryService.findByName(user.getId(), parsed.category(), parsed.type())
                    .orElse(null);
        }

        // Case 1: Category is known and matched
        if (category != null) {
            TransactionDraft draft = draftService.createDraft(
                    user,
                    parsed.type(),
                    parsed.amount(),
                    category,
                    parsed.description(),
                    TransactionSource.VOICE,
                    transcribedText,
                    parsed.confidence()
            );

            DraftDto dto = draftService.toDto(draft);
            java.time.LocalDate today = com.hisobchi.bot.common.util.DateTimeUtils.today(user.getTimezone());
            boolean isOffDay = dailyProfitService.isOffDay(user.getId(), today);
            String lastWorkText = null;
            if (isOffDay) {
                lastWorkText = dailyProfitService.getLastWorkedDayProfit(user.getId(), today)
                        .map(p -> com.hisobchi.bot.common.util.DateTimeUtils.formatUzbekDate(p.getProfitDate()))
                        .orElse("oldingi ishlagan kun");
            }
            boolean hasEnteredProfitToday = dailyProfitService.hasEnteredProfitToday(user, today);

            String confirmMsg = BotMessageBuilder.buildDraftConfirmationMessage(dto, user.getTimezone(), isOffDay, lastWorkText, hasEnteredProfitToday);
            apiClient.sendMessage(chatId, confirmMsg,
                    inlineKeyboardFactory.getDraftConfirmationKeyboard(draft.getId(), draft.getType(), isOffDay, lastWorkText, hasEnteredProfitToday), "HTML");
            return;
        }

        // Case 2: Amount found, but category is unknown / ambiguous (Section 11)
        TransactionDraft draft = draftService.createDraft(
                user,
                parsed.type(),
                parsed.amount(),
                null,
                parsed.description(),
                TransactionSource.VOICE,
                transcribedText,
                parsed.confidence()
        );

        List<Category> categories = categoryService.getCategories(user.getId(), parsed.type());
        String prompt = "🎙 <b>" + MoneyFormatter.format(parsed.amount()) + "</b> " +
                (parsed.type() == TransactionType.INCOME ? "daromad" : "xarajat") +
                " deb tushundim.\n\n<b>Qaysi kategoriyaga qo‘shay?</b>";

        apiClient.sendMessage(chatId, prompt,
                inlineKeyboardFactory.getCategorySelectionKeyboard(draft.getId(), categories), "HTML");
    }
}
