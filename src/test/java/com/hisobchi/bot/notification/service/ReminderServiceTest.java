package com.hisobchi.bot.notification.service;

import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.entity.ReminderType;
import com.hisobchi.bot.notification.repository.ReminderDeliveryLogRepository;
import com.hisobchi.bot.profit.entity.DailyProfit;
import com.hisobchi.bot.profit.service.DailyProfitService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardMarkup;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReminderDeliveryLogRepository deliveryLogRepository;
    @Mock
    private DailyProfitService dailyProfitService;
    @Mock
    private NotificationSettingsService settingsService;
    @Mock
    private TelegramApiClient apiClient;

    @InjectMocks
    private ReminderService reminderService;

    private User testUser;
    private NotificationSettings settings;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .telegramId(123456L)
                .timezone("Asia/Tashkent")
                .build();

        settings = NotificationSettings.builder()
                .id(1L)
                .user(testUser)
                .profitReminder18Enabled(true)
                .profitReminder21Enabled(true)
                .build();
    }

    @Test
    @DisplayName("18:00 reminder includes 'Bugungi foyda shu' button when profit is already entered")
    void test18ReminderHasConfirmProfitButtonWhenProfitEntered() {
        DailyProfit profit = DailyProfit.builder()
                .id(10L)
                .user(testUser)
                .profitDate(LocalDate.now())
                .totalProfit(new BigDecimal("10000"))
                .isWorkDay(true)
                .build();

        when(deliveryLogRepository.existsByUserIdAndReminderTypeAndReminderDate(any(), any(), any())).thenReturn(false);
        when(dailyProfitService.getProfit(eq(testUser.getId()), any())).thenReturn(Optional.of(profit));

        reminderService.sendReminder18IfEligible(testUser, LocalDate.now());

        ArgumentCaptor<InlineKeyboardMarkup> keyboardCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);
        verify(apiClient, times(1)).sendMessage(eq(testUser.getTelegramId()), anyString(), keyboardCaptor.capture(), eq("HTML"));

        InlineKeyboardMarkup markup = keyboardCaptor.getValue();
        assertNotNull(markup);
        assertTrue(markup.getInlineKeyboard().stream()
                .flatMap(List::stream)
                .anyMatch(btn -> "✅ Bugungi foyda shu".equals(btn.getText()) && "reminder:confirm_profit".equals(btn.getCallbackData())));
    }
}
