package com.hisobchi.bot.scheduler;

import com.hisobchi.bot.notification.service.PeriodicReportSenderService;
import com.hisobchi.bot.summary.service.DailyAutoCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final PeriodicReportSenderService reportSenderService;
    private final DailyAutoCloseService dailyAutoCloseService;

    // Run every minute at 00 seconds
    @Scheduled(cron = "0 * * * * *")
    public void runReportCheck() {
        log.trace("Running scheduled periodic report check...");
        reportSenderService.processReportsForCurrentMinute();
    }

    // Run every day at 23:59:00 Tashkent time to auto close daily statistics
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Tashkent")
    public void runDailyAutoClose() {
        log.info("Running automatic daily summary close for all users at 23:59...");
        dailyAutoCloseService.autoCloseAllUsersCurrentDay();
    }
}
