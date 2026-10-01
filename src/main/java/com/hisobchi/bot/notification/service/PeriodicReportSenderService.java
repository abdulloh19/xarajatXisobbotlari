package com.hisobchi.bot.notification.service;

import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.entity.ReportDeliveryLog;
import com.hisobchi.bot.notification.entity.ReportType;
import com.hisobchi.bot.notification.repository.ReportDeliveryLogRepository;
import com.hisobchi.bot.report.dto.ReportData;
import com.hisobchi.bot.report.service.ReportService;
import com.hisobchi.bot.telegram.client.TelegramApiClient;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardButton;
import com.hisobchi.bot.telegram.client.model.TelegramModels.InlineKeyboardMarkup;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodicReportSenderService {

    private final UserRepository userRepository;
    private final NotificationSettingsService settingsService;
    private final ReportDeliveryLogRepository reportDeliveryLogRepository;
    private final ReportService reportService;
    private final TelegramApiClient apiClient;

    @Transactional
    public void processReportsForCurrentMinute() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                processUserReports(user);
            } catch (Exception e) {
                log.error("Failed to process periodic reports for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
    }

    private void processUserReports(User user) {
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Tashkent");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalTime time = now.toLocalTime();
        LocalDate today = now.toLocalDate();

        NotificationSettings settings = settingsService.getOrCreateSettings(user);

        // 1. Daily Report at 23:00 (or user configured time)
        if (Boolean.TRUE.equals(settings.getDailyReportEnabled())) {
            String reportTime = settings.getDailyReportTime() != null ? settings.getDailyReportTime() : "23:00";
            String[] parts = reportTime.split(":");
            int targetHour = Integer.parseInt(parts[0]);
            int targetMin = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            if (time.getHour() == targetHour && time.getMinute() == targetMin) {
                sendDailyReportIfEligible(user, today);
            }
        }

        // 2. Weekly Report (Sunday 22:00)
        if (Boolean.TRUE.equals(settings.getWeeklyReportEnabled())
                && today.getDayOfWeek() == DayOfWeek.SUNDAY
                && time.getHour() == 22 && time.getMinute() == 0) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            sendPeriodReportIfEligible(user, ReportType.WEEKLY, monday, today, "HAFTALIK HISOBOT");
        }

        // 3. 2-Week Report (Every 14 days - 14th and 28th of the month at 22:15)
        if (Boolean.TRUE.equals(settings.getTwoWeekReportEnabled())
                && (today.getDayOfMonth() == 14 || today.getDayOfMonth() == 28)
                && time.getHour() == 22 && time.getMinute() == 15) {
            LocalDate start = today.minusDays(13);
            sendPeriodReportIfEligible(user, ReportType.TWO_WEEK, start, today, "2 HAFTALIK HISOBOT");
        }

        // 4. 3-Week Report (Every 21 days - 21st of the month at 22:20)
        if (Boolean.TRUE.equals(settings.getThreeWeekReportEnabled())
                && today.getDayOfMonth() == 21
                && time.getHour() == 22 && time.getMinute() == 20) {
            LocalDate start = today.minusDays(20);
            sendPeriodReportIfEligible(user, ReportType.THREE_WEEK, start, today, "3 HAFTALIK HISOBOT");
        }

        // 5. Monthly Report (Last day of month at 22:30)
        if (Boolean.TRUE.equals(settings.getMonthlyReportEnabled())
                && today.equals(today.with(TemporalAdjusters.lastDayOfMonth()))
                && time.getHour() == 22 && time.getMinute() == 30) {
            LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
            String title = today.getMonth().name() + " " + today.getYear() + " HISOBOTI";
            sendPeriodReportIfEligible(user, ReportType.MONTHLY, firstDay, today, title);
        }
    }

    private void sendDailyReportIfEligible(User user, LocalDate today) {
        if (reportDeliveryLogRepository.existsByUserIdAndReportTypeAndPeriodStartAndPeriodEnd(
                user.getId(), ReportType.DAILY, today, today)) {
            return;
        }

        ReportData data = reportService.getDailyReportData(user.getId(), today);
        String message = reportService.formatDailyReport(data);

        InlineKeyboardMarkup keyboard = null;
        if (!data.profitEntered()) {
            keyboard = InlineKeyboardMarkup.builder()
                    .inlineKeyboard(List.of(
                            List.of(InlineKeyboardButton.builder().text("✅ Foydani kiritish").callbackData("profit:enter").build())
                    ))
                    .build();
        }

        apiClient.sendMessage(user.getTelegramId(), message, keyboard, "HTML");

        reportDeliveryLogRepository.save(ReportDeliveryLog.builder()
                .user(user)
                .reportType(ReportType.DAILY)
                .periodStart(today)
                .periodEnd(today)
                .build());
        log.info("Delivered daily report to user {}", user.getId());
    }

    private void sendPeriodReportIfEligible(User user, ReportType type, LocalDate start, LocalDate end, String title) {
        if (reportDeliveryLogRepository.existsByUserIdAndReportTypeAndPeriodStartAndPeriodEnd(
                user.getId(), type, start, end)) {
            return;
        }

        ReportData data = reportService.getPeriodReportData(user.getId(), start, end, title);
        String message = reportService.formatPeriodReport(data);

        apiClient.sendMessage(user.getTelegramId(), message, null, "HTML");

        reportDeliveryLogRepository.save(ReportDeliveryLog.builder()
                .user(user)
                .reportType(type)
                .periodStart(start)
                .periodEnd(end)
                .build());
        log.info("Delivered {} report to user {} for period {} to {}", type, user.getId(), start, end);
    }
}
