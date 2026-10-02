package com.hisobchi.bot.telegram.keyboard;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.common.formatter.MoneyFormatter;
import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.telegram.client.model.TelegramModels.*;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                                new InlineKeyboardButton("📂 Kategoriyani o‘zgartirish", "tx:edit_field:" + transactionId + ":cat"),
                                new InlineKeyboardButton("✏️ Tahrirlash", "tx:edit:" + transactionId)
                        ),
                        List.of(
                                new InlineKeyboardButton("🗑 O‘chirish", "tx:delete_ask:" + transactionId),
                                new InlineKeyboardButton("🧾 Operatsiyalar", "tx:list_today")
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

    public InlineKeyboardMarkup getTransactionCategorySelectionKeyboard(Long transactionId, List<Category> categories) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> currentRow = new ArrayList<>();

        for (Category cat : categories) {
            String text = (cat.getEmoji() != null ? cat.getEmoji() + " " : "") + cat.getName();
            currentRow.add(new InlineKeyboardButton(text, "tx:set_cat:" + transactionId + ":" + cat.getId()));

            if (currentRow.size() == 2) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        rows.add(List.of(
                new InlineKeyboardButton("⬅️ Orqaga", "tx:detail:" + transactionId)
        ));

        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getSavedTransactionKeyboard(Long transactionId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("📂 Kategoriyani o‘zgartirish", "tx:edit_field:" + transactionId + ":cat"),
                                new InlineKeyboardButton("✏️ Tahrirlash", "tx:detail:" + transactionId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDailyOperationsKeyboard(List<Transaction> transactions, LocalDate date) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (transactions != null) {
            for (Transaction tx : transactions) {
                String emoji = (tx.getCategory() != null && tx.getCategory().getEmoji() != null)
                        ? tx.getCategory().getEmoji() : "📌";
                String name = (tx.getCategory() != null) ? tx.getCategory().getName() : "Boshqa";
                String prefix = tx.getType() == TransactionType.INCOME ? "+" : "-";
                String label = emoji + " " + name + " (" + prefix + MoneyFormatter.format(tx.getAmount()) + ")";
                rows.add(List.of(
                        new InlineKeyboardButton(label, "tx:detail:" + tx.getId())
                ));
            }
        }
        rows.add(List.of(
                new InlineKeyboardButton("⬅️ Orqaga", "report:daily")
        ));
        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getDailyReportActionsKeyboard(LocalDate date) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("🧾 Operatsiyalarni ko‘rish / tahrirlash", "tx:list_date:" + date),
                                new InlineKeyboardButton("💵 Foydani o‘zgartirish", "profit:enter")
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
                                new InlineKeyboardButton("✏️ Tahrirlash", "debt:edit_ask:" + draftId)
                        ),
                        List.of(
                                new InlineKeyboardButton("❌ Bekor qilish", "debt:cancel:" + draftId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtCreateMethodKeyboard() {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💵 Naqd", "debt_create:method:cash"),
                                new InlineKeyboardButton("💳 Karta", "debt_create:method:card")
                        ),
                        List.of(
                                new InlineKeyboardButton("❌ Bekor qilish", "debt:cancel_create")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtCreateEditFieldsKeyboard(Long draftId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💰 Summani o‘zgartirish", "debt:edit_field:" + draftId + ":amount"),
                                new InlineKeyboardButton("👤 Ismni o‘zgartirish", "debt:edit_field:" + draftId + ":person")
                        ),
                        List.of(
                                new InlineKeyboardButton("📅 Sanani o‘zgartirish", "debt:edit_field:" + draftId + ":date"),
                                new InlineKeyboardButton("💳 Pul turini o‘zgartirish", "debt:edit_field:" + draftId + ":method")
                        ),
                        List.of(
                                new InlineKeyboardButton("⬅️ Orqaga", "debt:edit_back:" + draftId),
                                new InlineKeyboardButton("❌ Bekor qilish", "debt:cancel:" + draftId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtSelectionKeyboard(List<Debt> debts, String prefix) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Debt d : debts) {
            String label = "👤 " + d.getPersonName() + " — " + com.hisobchi.bot.common.formatter.MoneyFormatter.format(d.getRemainingAmount()) + " qoldi";
            rows.add(List.of(new InlineKeyboardButton(label, prefix + ":select:" + d.getId())));
        }
        rows.add(List.of(new InlineKeyboardButton("❌ Bekor qilish", prefix + ":cancel")));
        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getDebtGroupedSelectionKeyboard(Map<String, List<Debt>> grouped, String prefix) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Map.Entry<String, List<Debt>> entry : grouped.entrySet()) {
            String person = entry.getKey();
            List<Debt> pDebts = entry.getValue();
            if (pDebts.size() == 1) {
                Debt d = pDebts.get(0);
                String label = "👤 " + d.getPersonName() + " — " + com.hisobchi.bot.common.formatter.MoneyFormatter.format(d.getRemainingAmount()) + " qoldi";
                rows.add(List.of(new InlineKeyboardButton(label, prefix + ":select:" + d.getId())));
            } else {
                java.math.BigDecimal total = pDebts.stream()
                        .map(Debt::getRemainingAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                String label = "👤 " + person + " — jami " + com.hisobchi.bot.common.formatter.MoneyFormatter.format(total);
                rows.add(List.of(new InlineKeyboardButton(label, prefix + ":person:" + person)));
            }
        }
        rows.add(List.of(new InlineKeyboardButton("❌ Bekor qilish", prefix + ":cancel")));
        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getDebtSubSelectionKeyboard(List<Debt> debts, String prefix) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM");
        for (Debt d : debts) {
            String dateStr = d.getDueDate() != null ? d.getDueDate().format(fmt) : (d.getStartDate() != null ? d.getStartDate().format(fmt) : "");
            String label = "📅 " + dateStr + " — " + com.hisobchi.bot.common.formatter.MoneyFormatter.format(d.getRemainingAmount()) + " qoldi";
            rows.add(List.of(new InlineKeyboardButton(label, prefix + ":select:" + d.getId())));
        }
        rows.add(List.of(new InlineKeyboardButton("⬅️ Orqaga", prefix + ":back_persons")));
        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getDebtAmountChoiceKeyboard(Long debtId, java.math.BigDecimal remaining, String prefix) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💯 Hammasini — " + com.hisobchi.bot.common.formatter.MoneyFormatter.format(remaining), prefix + ":full:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("✍️ Boshqa summa", prefix + ":custom:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("⬅️ Orqaga", prefix + ":back")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtPaymentMethodSelectionKeyboard(Long debtId, String prefix) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💵 Naqd", prefix + ":method:" + debtId + ":cash"),
                                new InlineKeyboardButton("💳 Karta", prefix + ":method:" + debtId + ":card")
                        ),
                        List.of(
                                new InlineKeyboardButton("⬅️ Orqaga", prefix + ":back_amt:" + debtId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtCreationPaymentMethodKeyboard() {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💵 Naqd", "debt_create:method:cash"),
                                new InlineKeyboardButton("💳 Karta", "debt_create:method:card")
                        ),
                        List.of(
                                new InlineKeyboardButton("❌ Bekor qilish", "debt:cancel_create")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtPaymentConfirmationKeyboard(Long debtId, String prefix, boolean isPay) {
        String confirmText = isPay ? "✅ To‘lash" : "✅ Saqlash";
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton(confirmText, prefix + ":confirm:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("✏️ Summani o‘zgartirish", prefix + ":custom:" + debtId),
                                new InlineKeyboardButton("❌ Bekor qilish", prefix + ":cancel")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtFullPaymentConfirmationKeyboard(Long debtId, String prefix, boolean isPay) {
        String confirmText = isPay ? "✅ Ha, to‘lash" : "✅ Ha, qabul qilindi";
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton(confirmText, prefix + ":confirm_full:" + debtId),
                                new InlineKeyboardButton("❌ Bekor qilish", prefix + ":cancel")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getOverpaymentBlockKeyboard(Long debtId, java.math.BigDecimal remaining, String prefix, boolean isPay) {
        String fullBtn = isPay ? "💯 " + com.hisobchi.bot.common.formatter.MoneyFormatter.format(remaining) + " ni to‘lash"
                               : "💯 " + com.hisobchi.bot.common.formatter.MoneyFormatter.format(remaining) + " ni qaytarish";
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton(fullBtn, prefix + ":full:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("✏️ Boshqa summa", prefix + ":custom:" + debtId),
                                new InlineKeyboardButton("❌ Bekor qilish", prefix + ":cancel")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getDebtActionKeyboard(Debt debt) {
        if (debt.getType() == DebtType.BORROWED) {
            return InlineKeyboardMarkup.builder()
                    .inlineKeyboard(List.of(
                            List.of(
                                    new InlineKeyboardButton("💳 Qarz to‘lash", "debt_pay:start:" + debt.getId()),
                                    new InlineKeyboardButton("💯 To‘liq yopish", "debt_pay:full_start:" + debt.getId())
                            ),
                            List.of(
                                    new InlineKeyboardButton("📅 Muddatni o‘zgartirish", "debt:extend:" + debt.getId()),
                                    new InlineKeyboardButton("📜 To‘lovlar tarixi", "debt:history:" + debt.getId())
                            ),
                            List.of(
                                    new InlineKeyboardButton("✏️ Tahrirlash", "debt:edit_ask:" + debt.getId()),
                                    new InlineKeyboardButton("⬅️ Orqaga", "debt:back")
                            )
                    ))
                    .build();
        } else {
            return InlineKeyboardMarkup.builder()
                    .inlineKeyboard(List.of(
                            List.of(
                                    new InlineKeyboardButton("💵 Qarz qaytardi", "debt_ret:start:" + debt.getId()),
                                    new InlineKeyboardButton("💯 To‘liq qaytardi", "debt_ret:full_start:" + debt.getId())
                            ),
                            List.of(
                                    new InlineKeyboardButton("📅 Muddatni uzaytirish", "debt:extend:" + debt.getId()),
                                    new InlineKeyboardButton("📜 Qaytarishlar tarixi", "debt:history:" + debt.getId())
                            ),
                            List.of(
                                    new InlineKeyboardButton("✏️ Tahrirlash", "debt:edit_ask:" + debt.getId()),
                                    new InlineKeyboardButton("⬅️ Orqaga", "debt:back")
                            )
                    ))
                    .build();
        }
    }

    public InlineKeyboardMarkup getDebtDetailPageKeyboard(Debt debt) {
        return getDebtActionKeyboard(debt);
    }

    public InlineKeyboardMarkup getDebtActionKeyboard(Long debtId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💳 Qarz to‘lash / qaytarish", "debt_pay:start:" + debtId),
                                new InlineKeyboardButton("📅 Muddatni uzaytirish", "debt:extend:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("📜 To‘lovlar tarixi", "debt:history:" + debtId),
                                new InlineKeyboardButton("⬅️ Orqaga", "debt:back")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getBorrowedReminderKeyboard(Long debtId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💯 Hammasini to‘ladim", "debt_pay:full_start:" + debtId),
                                new InlineKeyboardButton("💳 Qisman to‘ladim", "debt_pay:start:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("⏰ Hali yo‘q", "debt:dismiss:" + debtId),
                                new InlineKeyboardButton("📅 Muddatni o‘zgartirish", "debt:extend:" + debtId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getLentReminderKeyboard(Long debtId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("💯 Hammasini qaytardi", "debt_ret:full_start:" + debtId),
                                new InlineKeyboardButton("💵 Qisman qaytardi", "debt_ret:start:" + debtId)
                        ),
                        List.of(
                                new InlineKeyboardButton("⏰ Hali qaytmadi", "debt:dismiss:" + debtId),
                                new InlineKeyboardButton("📅 Muddat berish", "debt:extend:" + debtId)
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getVoiceFullRepayKeyboard(Long debtId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Ha, hammasini to‘ladim", "debt_voice:pay_full:" + debtId),
                                new InlineKeyboardButton("❌ Yo‘q", "debt_voice:cancel")
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getVoiceFullReturnKeyboard(Long debtId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                new InlineKeyboardButton("✅ Ha, hammasini qaytardi", "debt_voice:ret_full:" + debtId),
                                new InlineKeyboardButton("❌ Yo‘q", "debt_voice:cancel")
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
