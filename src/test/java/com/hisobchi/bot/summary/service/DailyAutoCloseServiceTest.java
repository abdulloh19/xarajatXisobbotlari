package com.hisobchi.bot.summary.service;

import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.service.StatisticsService;
import com.hisobchi.bot.summary.entity.DailySummary;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyAutoCloseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DailySummaryService dailySummaryService;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private DailyAutoCloseService dailyAutoCloseService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .telegramId(12345L)
                .timezone("Asia/Tashkent")
                .build();
    }

    @Test
    @DisplayName("autoClosePastDays should close previous day if not closed")
    void testAutoClosePastDaysWhenUnclosed() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(dailySummaryService.isDayClosed(eq(testUser.getId()), any(LocalDate.class))).thenReturn(false);

        DailyStatisticsDto mockStats = new DailyStatisticsDto(
                yesterday,
                new BigDecimal("500000"),
                new BigDecimal("100000"),
                new BigDecimal("400000"),
                5,
                Collections.emptyList(),
                false
        );
        when(statisticsService.getDailyStatistics(eq(testUser), any(LocalDate.class))).thenReturn(mockStats);

        dailyAutoCloseService.autoClosePastDays(testUser);

        verify(dailySummaryService, times(1)).closeDay(
                eq(testUser),
                any(LocalDate.class),
                eq(new BigDecimal("500000")),
                eq(new BigDecimal("100000")),
                eq(new BigDecimal("400000"))
        );
    }

    @Test
    @DisplayName("autoClosePastDays should do nothing if previous day is already closed")
    void testAutoClosePastDaysWhenAlreadyClosed() {
        when(dailySummaryService.isDayClosed(eq(testUser.getId()), any(LocalDate.class))).thenReturn(true);

        dailyAutoCloseService.autoClosePastDays(testUser);

        verify(dailySummaryService, never()).closeDay(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("autoCloseAllUsersCurrentDay should close current day for all users")
    void testAutoCloseAllUsersCurrentDay() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(dailySummaryService.isDayClosed(eq(testUser.getId()), any(LocalDate.class))).thenReturn(false);

        DailyStatisticsDto mockStats = new DailyStatisticsDto(
                LocalDate.now(),
                new BigDecimal("300000"),
                new BigDecimal("50000"),
                new BigDecimal("250000"),
                3,
                Collections.emptyList(),
                false
        );
        when(statisticsService.getDailyStatistics(eq(testUser), any(LocalDate.class))).thenReturn(mockStats);

        dailyAutoCloseService.autoCloseAllUsersCurrentDay();

        verify(dailySummaryService, times(1)).closeDay(
                eq(testUser),
                any(LocalDate.class),
                eq(new BigDecimal("300000")),
                eq(new BigDecimal("50000")),
                eq(new BigDecimal("250000"))
        );
    }
}
