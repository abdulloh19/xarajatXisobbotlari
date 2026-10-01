package com.hisobchi.bot.telegram.keyboard;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InlineKeyboardFactory {

    public InlineKeyboardMarkup getDraftConfirmationKeyboard(Long draftId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Saqlash", "draft:save:" + draftId),
                                new InlineKeyboardButton("✏️ Tahrirlash", "draft:edit:" + draftId)
                        ),
                        List.of(
                                new InlineKeyboardButton("❌ Bekor qilish", "draft:cancel:" + draftId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getIntentSelectionKeyboard(Long draftId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💸 Xarajat", "draft:intent:" + draftId + ":EXPENSE"),
                                new InlineKeyboardButton("💰 Daromad", "draft:intent:" + draftId + ":INCOME")
                        ),
                        List.of(
                                new InlineKeyboardButton("❌ Bekor qilish", "draft:cancel:" + draftId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getCategorySelectionKeyboard(Long draftId, List<Category> categories) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> currentRow = new ArrayList<>();

        for (Category cat : categories) {
            String text = (cat.getEmoji() != null ? cat.getEmoji() + " " : "") + cat.getName();
            currentRow.add(new InlineKeyboardButton(text, "draft:set_cat:" + draftId + ":" + cat.getId()));

            if (currentRow.size() == 2) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        rows.add(List.of(
                new InlineKeyboardButton("❌ Bekor qilish", "draft:cancel:" + draftId)
        ));

        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getEditDraftFieldsKeyboard(Long draftId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💰 Summani o‘zgartirish", "draft:edit_field:" + draftId + ":amount"),
                                new InlineKeyboardButton("📂 Kategoriyani o‘zgartirish", "draft:edit_field:" + draftId + ":cat")
                        ),
                        List.of(
                                new InlineKeyboardButton("📝 Izohni o‘zgartirish", "draft:edit_field:" + draftId + ":desc")
                        ),
                        List.of(
                                new InlineKeyboardButton("⬅️ Orqaga", "draft:back:" + draftId),
                                new InlineKeyboardButton("❌ Bekor qilish", "draft:cancel:" + draftId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getCloseDayConfirmationKeyboard() {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Kunni yopish", "day:close:confirm"),
                                new InlineKeyboardButton("❌ Bekor qilish", "day:close:cancel")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getReopenDayConfirmationKeyboard(LocalDate date) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("🔓 Qayta ochish", "day:reopen:confirm:" + date),
                                new InlineKeyboardButton("❌ Bekor qilish", "day:reopen:cancel")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getHistoryItemActionsKeyboard(Long transactionId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✏️ Tahrirlash", "tx:edit:" + transactionId),
                                new InlineKeyboardButton("🗑 O‘chirish", "tx:delete_ask:" + transactionId)
                        ),
                        List.of(
                                new InlineKeyboardButton("⬅️ Tarixga qaytish", "history:back")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDeleteConfirmationKeyboard(Long transactionId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Ha, o‘chirish", "tx:delete_confirm:" + transactionId),
                                new InlineKeyboardButton("❌ Yo‘q, bekor qilish", "tx:delete_cancel:" + transactionId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getEditTransactionFieldsKeyboard(Long transactionId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💰 Summani tahrirlash", "tx:edit_field:" + transactionId + ":amount"),
                                new InlineKeyboardButton("📂 Kategoriyani tahrirlash", "tx:edit_field:" + transactionId + ":cat")
                        ),
                        List.of(
                                new InlineKeyboardButton("📝 Izohni tahrirlash", "tx:edit_field:" + transactionId + ":desc")
                        ),
                        List.of(
                                new InlineKeyboardButton("⬅️ Orqaga", "tx:detail:" + transactionId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtConfirmationKeyboard(Long draftId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Saqlash", "debt:save:" + draftId)
                        ),
                        List.of(
                                new InlineKeyboardButton("💰 Summani o‘zgartirish", "debt:edit_field:" + draftId + ":amount"),
                                new InlineKeyboardButton("👤 Ismni o‘zgartirish", "debt:edit_field:" + draftId + ":person")
                        ),
                        List.of(
                                new InlineKeyboardButton("📅 Sanani o‘zgartirish", "debt:edit_field:" + draftId + ":date"),
                                new InlineKeyboardButton("❌ Bekor qilish", "debt:cancel:" + draftId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtActionKeyboard(Debt debt) {
        String resolveText = debt.getType() == DebtType.BORROWED ? "✅ To‘ladim" : "✅ Qaytarib oldim";
        String extendText = debt.getType() == DebtType.BORROWED ? "📅 Muddatni o‘zgartirish" : "📅 Muddatni uzaytirish";
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton(resolveText, "debt:pay:" + debt.getId()),
                                new InlineKeyboardButton(extendText, "debt:extend:" + debt.getId())
                        ),
                        List.of(
                                new InlineKeyboardButton("🗑 O‘chirish", "debt:del:" + debt.getId()),
                                new InlineKeyboardButton("⬅️ Orqaga", "debt:back")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtActionKeyboard(Long debtId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Qaytarildi / Yopish", "debt:pay:" + debtId),
                                new InlineKeyboardButton("📅 Muddatni uzaytirish", "debt:extend:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("🗑 O‘chirish", "debt:del:" + debtId),
                                new InlineKeyboardButton("⬅️ Orqaga", "debt:back")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtDateExtensionConfirmKeyboard(Long debtId, LocalDate newDate) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Tasdiqlash", "debt:conf_ext:" + debtId + ":" + newDate.toString()),
                                new InlineKeyboardButton("❌ Bekor qilish", "debt:cancel_ext")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getNotificationSettingsKeyboard(NotificationSettings s) {
        String icon18 = Boolean.TRUE.equals(s.getProfitReminder18Enabled()) ? "✅" : "❌";
        String icon21 = Boolean.TRUE.equals(s.getProfitReminder21Enabled()) ? "✅" : "❌";
        String iconDaily = Boolean.TRUE.equals(s.getDailyReportEnabled()) ? "✅" : "❌";
        String iconWeekly = Boolean.TRUE.equals(s.getWeeklyReportEnabled()) ? "✅" : "❌";
        String icon2w = Boolean.TRUE.equals(s.getTwoWeekReportEnabled()) ? "✅" : "❌";
        String icon3w = Boolean.TRUE.equals(s.getThreeWeekReportEnabled()) ? "✅" : "❌";
        String iconMonth = Boolean.TRUE.equals(s.getMonthlyReportEnabled()) ? "✅" : "❌";

        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(new InlineKeyboardButton(icon18 + " 18:00 foyda eslatmasi", "notif:toggle:rem_18")),
                        List.of(new InlineKeyboardButton(icon21 + " 21:00 foyda eslatmasi", "notif:toggle:rem_21")),
                        List.of(new InlineKeyboardButton(iconDaily + " Kunlik hisobot (23:00)", "notif:toggle:rep_daily")),
                        List.of(new InlineKeyboardButton(iconWeekly + " Haftalik hisobot (Yakshanba)", "notif:toggle:rep_weekly")),
                        List.of(new InlineKeyboardButton(icon2w + " 2 haftalik hisobot", "notif:toggle:rep_2week")),
                        List.of(new InlineKeyboardButton(icon3w + " 3 haftalik hisobot", "notif:toggle:rep_3week")),
                        List.of(new InlineKeyboardButton(iconMonth + " Oylik hisobot", "notif:toggle:rep_monthly")),
                        List.of(new InlineKeyboardButton("⬅️ Sozlamalarga qaytish", "notif:back"))
                ))
                .build();
    }
}
