package com.hisobchi.bot.scheduler;

import com.hisobchi.bot.notification.service.PeriodicReportSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final PeriodicReportSenderService reportSenderService;

    // Run every minute at 00 seconds
    @Scheduled(cron = "0 * * * * *")
    public void runReportCheck() {
        log.trace("Running scheduled periodic report check...");
        reportSenderService.processReportsForCurrentMinute();
    }
}
