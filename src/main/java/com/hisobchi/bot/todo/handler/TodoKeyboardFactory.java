package com.hisobchi.bot.todo.handler;

import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardButton;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardMarkup;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.TodoSubtask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TodoKeyboardFactory {

    public InlineKeyboardMarkup getTaskCardKeyboard(TodoTaskDto task) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (task.status() != com.hisobchi.bot.todo.entity.TodoStatus.COMPLETED) {
            rows.add(List.of(
                    InlineKeyboardButton.builder().text("✅ Bajarildi").callbackData("todo:done:" + task.id()).build(),
                    InlineKeyboardButton.builder().text("⏰ Keyinroq").callbackData("todo:snooze_menu:" + task.id()).build()
            ));
            rows.add(List.of(
                    InlineKeyboardButton.builder().text("📋 Kichik ishlar (" + task.completedSubtasks() + "/" + task.totalSubtasks() + ")")
                            .callbackData("todo:subtasks:" + task.id()).build(),
                    InlineKeyboardButton.builder().text("✏️ Tahrirlash").callbackData("todo:edit_menu:" + task.id()).build()
            ));
            rows.add(List.of(
                    InlineKeyboardButton.builder().text("❌ Bekor qilish").callbackData("todo:cancel:" + task.id()).build(),
                    InlineKeyboardButton.builder().text("🗑 O‘chirish").callbackData("todo:delete:" + task.id()).build()
            ));
        } else {
            rows.add(List.of(
                    InlineKeyboardButton.builder().text("🔄 Qayta ochish").callbackData("todo:reopen:" + task.id()).build(),
                    InlineKeyboardButton.builder().text("🗑 O‘chirish").callbackData("todo:delete:" + task.id()).build()
            ));
        }

        rows.add(List.of(
                InlineKeyboardButton.builder().text("📋 Ro‘yxatga qaytish").callbackData("todo:list:today").build()
        ));

        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getTaskCreatedKeyboard(Long taskId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("👁 Ko‘rish").callbackData("todo:view:" + taskId).build(),
                                InlineKeyboardButton.builder().text("❌ Bekor qilish (Undo)").callbackData("todo:delete:" + taskId).build()
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getExpenseConversionKeyboard(Long taskId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("✅ Xarajatga yozish").callbackData("todo:exp_yes:" + taskId).build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("✏️ Summani o‘zgartirish").callbackData("todo:exp_edit:" + taskId).build(),
                                InlineKeyboardButton.builder().text("✔️ Faqat vazifani yopish").callbackData("todo:exp_skip:" + taskId).build()
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getExpenseRecordedKeyboard(Long taskId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("↩️ Xarajatni bekor qilish").callbackData("todo:exp_undo:" + taskId).build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("📋 Vazifalar").callbackData("todo:list:today").build(),
                                InlineKeyboardButton.builder().text("🏠 Asosiy menyu").callbackData("menu:main").build()
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getSnoozeMenuKeyboard(Long taskId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("⏰ +15 daqiqa").callbackData("todo:snooze:" + taskId + ":15").build(),
                                InlineKeyboardButton.builder().text("⏰ +1 soat").callbackData("todo:snooze:" + taskId + ":60").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("⏰ +3 soat").callbackData("todo:snooze:" + taskId + ":180").build(),
                                InlineKeyboardButton.builder().text("📅 Ertaga 09:00").callbackData("todo:snooze_tmr:" + taskId).build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("⬅️ Orqaga").callbackData("todo:view:" + taskId).build()
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getEditMenuKeyboard(Long taskId) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("📝 Nomini o‘zgartirish").callbackData("todo:edit_t:" + taskId).build(),
                                InlineKeyboardButton.builder().text("📅 Muddatni o‘zgartirish").callbackData("todo:edit_d:" + taskId).build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("💰 Summani o‘zgartirish").callbackData("todo:edit_a:" + taskId).build(),
                                InlineKeyboardButton.builder().text("🤖 AI orqali qismlarga bo‘lish").callbackData("todo:ai_split:" + taskId).build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("⬅️ Orqaga").callbackData("todo:view:" + taskId).build()
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getTaskListKeyboard(String activeFilter, int page, int totalPages, List<TodoTaskDto> tasks) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 1. Filter selection tabs
        rows.add(List.of(
                InlineKeyboardButton.builder().text(("today".equals(activeFilter) ? "🔘 " : "") + "📅 Bugun").callbackData("todo:list:today:0").build(),
                InlineKeyboardButton.builder().text(("week".equals(activeFilter) ? "🔘 " : "") + "📆 7 kun").callbackData("todo:list:week:0").build(),
                InlineKeyboardButton.builder().text(("overdue".equals(activeFilter) ? "🔘 " : "") + "⚠️ Kechikkan").callbackData("todo:list:overdue:0").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text(("nodate".equals(activeFilter) ? "🔘 " : "") + "📝 Sanasiz").callbackData("todo:list:nodate:0").build(),
                InlineKeyboardButton.builder().text(("done".equals(activeFilter) ? "🔘 " : "") + "✅ Bajarilgan").callbackData("todo:list:done:0").build(),
                InlineKeyboardButton.builder().text("📁 Loyihalar").callbackData("todo:proj:list").build()
        ));

        // 2. Quick task selector buttons (1..N)
        if (!tasks.isEmpty()) {
            List<InlineKeyboardButton> numRow = new ArrayList<>();
            for (int i = 0; i < tasks.size(); i++) {
                numRow.add(InlineKeyboardButton.builder()
                        .text("№ " + (i + 1))
                        .callbackData("todo:view:" + tasks.get(i).id())
                        .build());
                if (numRow.size() == 5 || i == tasks.size() - 1) {
                    rows.add(new ArrayList<>(numRow));
                    numRow.clear();
                }
            }
        }

        // 3. Pagination controls if more than 1 page
        if (totalPages > 1) {
            List<InlineKeyboardButton> navRow = new ArrayList<>();
            if (page > 0) {
                navRow.add(InlineKeyboardButton.builder().text("⬅️ Oldingi").callbackData("todo:list:" + activeFilter + ":" + (page - 1)).build());
            }
            navRow.add(InlineKeyboardButton.builder().text((page + 1) + " / " + totalPages).callbackData("noop").build());
            if (page < totalPages - 1) {
                navRow.add(InlineKeyboardButton.builder().text("Keyingi ➡️").callbackData("todo:list:" + activeFilter + ":" + (page + 1)).build());
            }
            rows.add(navRow);
        }

        // 4. Action buttons
        rows.add(List.of(
                InlineKeyboardButton.builder().text("➕ Yangi vazifa").callbackData("todo:add").build(),
                InlineKeyboardButton.builder().text("📊 Reja hisoboti").callbackData("todo:report:planned").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("🏠 Asosiy menyu").callbackData("menu:main").build()
        ));

        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getSubtasksKeyboard(Long taskId, List<TodoSubtask> subtasks) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (TodoSubtask s : subtasks) {
            String check = s.getCompleted() ? "✅ " : "⬜ ";
            rows.add(List.of(
                    InlineKeyboardButton.builder().text(check + s.getTitle()).callbackData("todo:st_toggle:" + s.getId()).build(),
                    InlineKeyboardButton.builder().text("🗑").callbackData("todo:st_del:" + s.getId()).build()
            ));
        }

        rows.add(List.of(
                InlineKeyboardButton.builder().text("➕ Kichik qadam qo‘shish").callbackData("todo:st_add:" + taskId).build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("⬅️ Vazifaga qaytish").callbackData("todo:view:" + taskId).build()
        ));

        return InlineKeyboardMarkup.builder().inlineKeyboard(rows).build();
    }

    public InlineKeyboardMarkup getAiDecompositionConfirmKeyboard(Long taskId, int subtaskCount) {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("✅ Barchasini saqlash (" + subtaskCount + " ta)").callbackData("todo:ai_save:" + taskId).build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("❌ Bekor qilish").callbackData("todo:view:" + taskId).build()
                        )
                ))
                .build();
    }

    public InlineKeyboardMarkup getProjectListKeyboard() {
        return InlineKeyboardMarkup.builder()
                .inlineKeyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("➕ Yangi loyiha yaratish").callbackData("todo:proj_add").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("📋 Barcha vazifalar").callbackData("todo:list:today").build(),
                                InlineKeyboardButton.builder().text("🏠 Asosiy menyu").callbackData("menu:main").build()
                        )
                ))
                .build();
    }
}
