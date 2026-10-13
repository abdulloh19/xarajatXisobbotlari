package com.hisobchi.bot.todo;

import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.service.NotificationSettingsService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.todo.entity.TodoPriority;
import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.repository.TodoReminderLogRepository;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.todo.service.TodoReminderService;
import com.hisobchi.bot.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoReminderServiceTest {

    @Mock
    private TodoTaskRepository taskRepository;
    @Mock
    private TodoReminderLogRepository reminderLogRepository;
    @Mock
    private NotificationSettingsService settingsService;
    @Mock
    private TelegramApiClient apiClient;

    @InjectMocks
    private TodoReminderService reminderService;

    private User testUser;
    private NotificationSettings settings;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .telegramId(123456789L)
                .timezone("Asia/Tashkent")
                .build();

        settings = NotificationSettings.builder()
                .id(1L)
                .user(testUser)
                .quietHoursEnabled(true)
                .quietHoursStart("00:00") // always within quiet hours for test if needed
                .quietHoursEnd("23:59")
                .build();
    }

    @Test
    @DisplayName("Tasks with explicit specific time bypass quiet hours and are delivered")
    void testSpecificTimeTasksBypassQuietHours() {
        when(settingsService.getOrCreateSettings(testUser)).thenReturn(settings);

        TodoTask specificTask = TodoTask.builder()
                .id(10L)
                .user(testUser)
                .title("23:00 dagi dorini ichish")
                .hasSpecificTime(true)
                .dueTime(LocalTime.of(23, 0))
                .dueDate(LocalDate.now())
                .status(TodoStatus.OPEN)
                .priority(TodoPriority.HIGH)
                .reminderAt(Instant.now())
                .build();

        when(taskRepository.findDueReminders(any(Instant.class))).thenReturn(List.of(specificTask));

        reminderService.processDueReminders();

        // Must deliver single reminder to Telegram
        verify(apiClient, times(1)).sendMessage(eq(testUser.getTelegramId()), any(), any(), eq("HTML"));
        // ReminderAt must be cleared after successful delivery
        assertNull(specificTask.getReminderAt());
        verify(taskRepository, times(1)).save(specificTask);
    }

    @Test
    @DisplayName("General tasks without specific time are postponed during quiet hours")
    void testGeneralTasksArePostponedDuringQuietHours() {
        when(settingsService.getOrCreateSettings(testUser)).thenReturn(settings);

        TodoTask generalTask = TodoTask.builder()
                .id(20L)
                .user(testUser)
                .title("Umumiy vazifa")
                .hasSpecificTime(false)
                .dueDate(LocalDate.now())
                .status(TodoStatus.OPEN)
                .priority(TodoPriority.MEDIUM)
                .reminderAt(Instant.now())
                .build();

        when(taskRepository.findDueReminders(any(Instant.class))).thenReturn(List.of(generalTask));

        reminderService.processDueReminders();

        // Must NOT send to Telegram
        verify(apiClient, never()).sendMessage(any(), any(), any(), any());
        // ReminderAt must be postponed to future instant (not null)
        assertNotNull(generalTask.getReminderAt());
        verify(taskRepository, times(1)).save(generalTask);
    }
}
