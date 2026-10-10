package com.hisobchi.bot.telegram.handler;

import com.hisobchi.bot.ai.dto.ParsedDebt;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtDraft;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.service.DebtDraftService;
import com.hisobchi.bot.debt.service.DebtFlowService;
import com.hisobchi.bot.debt.service.DebtService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.keyboard.InlineKeyboardFactory;
import com.hisobchi.bot.telegram.keyboard.ReplyKeyboardFactory;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserState;
import com.hisobchi.bot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtNlpHandler {

    private final TelegramApiClient apiClient;
    private final DebtService debtService;
    private final DebtDraftService debtDraftService;
    private final DebtFlowService debtFlowService;
    private final UserService userService;
    private final InlineKeyboardFactory inlineKeyboardFactory;
    private final ReplyKeyboardFactory replyKeyboardFactory;

    public void handleParsedDebt(User user, Long chatId, ParsedDebt d, String rawText) {
        log.info("Processing parsed debt NLP intent: {} for user: {}", d.intent(), user.getId());

        switch (d.intent()) {
            case REPAY_FULL -> {
                Optional<Debt> matchingOpt = debtService.findMatchingActiveDebt(user.getId(), d.personName(), DebtType.BORROWED);
                if (matchingOpt.isPresent()) {
                    Debt debt = matchingOpt.get();
                    debtFlowService.setSelectedDebtId(user.getId(), debt.getId());
                    String msg = String.format("""
                            🎙 <b>%sga qolgan qarzingiz:</b>

                            %s

                            Hammasini to‘ladingiz deb belgilaymi?
                            """,
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount())
                    );
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getVoiceFullRepayKeyboard(debt.getId()), "HTML");
                } else {
                    apiClient.sendMessage(chatId, "⚠️ <b>" + BotMessageBuilder.escapeHtml(d.personName()) + "</b> bo‘yicha faol olingan qarz topilmadi.",
                            replyKeyboardFactory.getDebtsMenu(), "HTML");
                }
            }
            case RETURN_FULL -> {
                Optional<Debt> matchingOpt = debtService.findMatchingActiveDebt(user.getId(), d.personName(), DebtType.LENT);
                if (matchingOpt.isPresent()) {
                    Debt debt = matchingOpt.get();
                    debtFlowService.setSelectedDebtId(user.getId(), debt.getId());
                    String msg = String.format("""
                            <b>%sning qolgan qarzi:</b>

                            %s

                            Hammasi qaytdi deb belgilaymi?
                            """,
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(debt.getRemainingAmount())
                    );
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getVoiceFullReturnKeyboard(debt.getId()), "HTML");
                } else {
                    apiClient.sendMessage(chatId, "⚠️ <b>" + BotMessageBuilder.escapeHtml(d.personName()) + "</b> bo‘yicha faol berilgan qarz topilmadi.",
                            replyKeyboardFactory.getDebtsMenu(), "HTML");
                }
            }
            case REPAY_PARTIAL -> {
                Optional<Debt> matchingOpt = debtService.findMatchingActiveDebt(user.getId(), d.personName(), DebtType.BORROWED);
                if (matchingOpt.isPresent()) {
                    Debt debt = matchingOpt.get();
                    BigDecimal paymentAmt = d.amount();
                    if (paymentAmt.compareTo(debt.getRemainingAmount()) > 0) {
                        String overMsg = String.format("""
                                ⚠️ <b>%sga qolgan qarzingiz:</b>
                                %s

                                Siz:
                                %s

                                kiritdingiz.

                                Qolgan qarzdan ko‘p summa kiritib bo‘lmaydi.
                                """,
                                BotMessageBuilder.escapeHtml(debt.getPersonName()),
                                MoneyFormatter.format(debt.getRemainingAmount()),
                                MoneyFormatter.format(paymentAmt)
                        );
                        apiClient.sendMessage(chatId, overMsg, inlineKeyboardFactory.getOverpaymentBlockKeyboard(debt.getId(), debt.getRemainingAmount(), "debt_pay", true), "HTML");
                        return;
                    }
                    debtFlowService.setSelectedDebtId(user.getId(), debt.getId());
                    debtFlowService.setPaymentAmount(user.getId(), paymentAmt);
                    BigDecimal newRemaining = debt.getRemainingAmount().subtract(paymentAmt);

                    String msg = String.format("""
                            🎙 <b>Tushundim</b>

                            👤 <b>%s</b>

                            💳 <b>Qarzdan to‘lov:</b>
                            %s

                            📌 <b>Oldingi qarz:</b>
                            %s

                            🔴 <b>To‘lovdan keyin qoladi:</b>
                            %s

                            Pul qayerdan berildi?
                            """,
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(paymentAmt),
                            MoneyFormatter.format(debt.getRemainingAmount()),
                            MoneyFormatter.format(newRemaining)
                    );
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debt.getId(), "debt_pay"), "HTML");
                } else {
                    apiClient.sendMessage(chatId, "⚠️ <b>" + BotMessageBuilder.escapeHtml(d.personName()) + "</b> bo‘yicha faol olingan qarz topilmadi.",
                            replyKeyboardFactory.getDebtsMenu(), "HTML");
                }
            }
            case RETURN_PARTIAL -> {
                Optional<Debt> matchingOpt = debtService.findMatchingActiveDebt(user.getId(), d.personName(), DebtType.LENT);
                if (matchingOpt.isPresent()) {
                    Debt debt = matchingOpt.get();
                    BigDecimal returnedAmt = d.amount();
                    if (returnedAmt.compareTo(debt.getRemainingAmount()) > 0) {
                        String overMsg = String.format("""
                                ⚠️ <b>%sning qolgan qarzi:</b>
                                %s

                                Siz:
                                %s

                                kiritdingiz.

                                Qolgan qarzdan ko‘p summa kiritib bo‘lmaydi.
                                """,
                                BotMessageBuilder.escapeHtml(debt.getPersonName()),
                                MoneyFormatter.format(debt.getRemainingAmount()),
                                MoneyFormatter.format(returnedAmt)
                        );
                        apiClient.sendMessage(chatId, overMsg, inlineKeyboardFactory.getOverpaymentBlockKeyboard(debt.getId(), debt.getRemainingAmount(), "debt_ret", false), "HTML");
                        return;
                    }
                    debtFlowService.setSelectedDebtId(user.getId(), debt.getId());
                    debtFlowService.setPaymentAmount(user.getId(), returnedAmt);
                    BigDecimal newRemaining = debt.getRemainingAmount().subtract(returnedAmt);

                    String msg = String.format("""
                            🎙 <b>Tushundim</b>

                            👤 <b>%s</b>

                            💰 <b>Qaytardi:</b>
                            %s

                            📌 <b>Oldingi qarzi:</b>
                            %s

                            🟢 <b>Endi qolgan qarzi:</b>
                            %s

                            Pul qayerga tushdi?
                            """,
                            BotMessageBuilder.escapeHtml(debt.getPersonName()),
                            MoneyFormatter.format(returnedAmt),
                            MoneyFormatter.format(debt.getRemainingAmount()),
                            MoneyFormatter.format(newRemaining)
                    );
                    apiClient.sendMessage(chatId, msg, inlineKeyboardFactory.getDebtPaymentMethodSelectionKeyboard(debt.getId(), "debt_ret"), "HTML");
                } else {
                    apiClient.sendMessage(chatId, "⚠️ <b>" + BotMessageBuilder.escapeHtml(d.personName()) + "</b> bo‘yicha faol berilgan qarz topilmadi.",
                            replyKeyboardFactory.getDebtsMenu(), "HTML");
                }
            }
            case LEND -> {
                if (d.missingPerson()) {
                    debtFlowService.setDebtType(user.getId(), DebtType.LENT);
                    debtFlowService.setDebtAmount(user.getId(), d.amount());
                    userService.updateState(user.getTelegramId(), UserState.WAITING_VOICE_DEBT_PERSON);
                    String msg = "🟢 <b>" + MoneyFormatter.format(d.amount()) + "</b> qarz berdingiz deb tushundim.\n\n👤 <b>Kimga berdingiz?</b>";
                    apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getCancelMenu(), "HTML");
                } else {
                    DebtDraft debtDraft = debtDraftService.createDraft(
                            user, d.type(), d.amount(), d.personName(), d.dueDate(), d.description(), rawText, d.confidence(),
                            d.paymentMethod() != null ? d.paymentMethod() : "Naqd", LocalDate.now()
                    );
                    String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(debtDraft);
                    apiClient.sendMessage(chatId, confirmMsg,
                            inlineKeyboardFactory.getDebtConfirmationKeyboard(debtDraft.getId(), debtDraft.getType()), "HTML");
                }
            }
            case BORROW -> {
                if (d.missingPerson()) {
                    debtFlowService.setDebtType(user.getId(), DebtType.BORROWED);
                    debtFlowService.setDebtAmount(user.getId(), d.amount());
                    userService.updateState(user.getTelegramId(), UserState.WAITING_VOICE_DEBT_PERSON);
                    String msg = "🔴 <b>" + MoneyFormatter.format(d.amount()) + "</b> qarz oldingiz (siz qarzsiz) deb tushundim.\n\n👤 <b>Kimdan yoki nimadan (masalan: Moy, Zapravka, Ali) qarz bo‘ldingiz?</b>";
                    apiClient.sendMessage(chatId, msg, replyKeyboardFactory.getCancelMenu(), "HTML");
                } else {
                    DebtDraft debtDraft = debtDraftService.createDraft(
                            user, d.type(), d.amount(), d.personName(), d.dueDate(), d.description(), rawText, d.confidence(),
                            d.paymentMethod() != null ? d.paymentMethod() : "Naqd", LocalDate.now()
                    );
                    String confirmMsg = BotMessageBuilder.buildDebtDraftConfirmationMessage(debtDraft);
                    apiClient.sendMessage(chatId, confirmMsg,
                            inlineKeyboardFactory.getDebtConfirmationKeyboard(debtDraft.getId(), debtDraft.getType()), "HTML");
                }
            }
        }
    }
}
