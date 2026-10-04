package com.hisobchi.bot.summary.service;

import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.statistics.dto.DailyStatisticsDto;
import com.hisobchi.bot.statistics.service.StatisticsService;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAutoCloseService {

    private final UserRepository userRepository;
    private final DailySummaryService dailySummaryService;
    private final StatisticsService statisticsService;

    @Transactional
    public void autoClosePastDays(User user) {
        String tz = user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent";
        LocalDate today = DateTimeUtils.today(tz);
        LocalDate yesterday = today.minusDays(1);

        if (!dailySummaryService.isDayClosed(user.getId(), yesterday)) {
            DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, yesterday);
            dailySummaryService.closeDay(user, yesterday, stats.totalIncome(), stats.totalExpense(), stats.netProfit());
            log.info("Auto-closed previous day {} for user id {}", yesterday, user.getId());
        }
    }

    @Transactional
    public void autoCloseAllUsersCurrentDay() {
        LocalDate today = DateTimeUtils.today("Asia/Tashkent");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                if (!dailySummaryService.isDayClosed(user.getId(), today)) {
                    DailyStatisticsDto stats = statisticsService.getDailyStatistics(user, today);
                    dailySummaryService.closeDay(user, today, stats.totalIncome(), stats.totalExpense(), stats.netProfit());
                    log.info("Midnight auto-closed day {} for user id {}", today, user.getId());
                }
            } catch (Exception e) {
                log.error("Failed to auto-close day {} for user {}: {}", today, user.getId(), e.getMessage());
            }
        }
    }
}
