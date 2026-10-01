package com.hisobchi.bot.scheduler;

import com.hisobchi.bot.notification.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    // Run every minute at 00 seconds
    @Scheduled(cron = "0 * * * * *")
    public void runReminderCheck() {
        log.trace("Running scheduled reminder check...");
        reminderService.processRemindersForCurrentMinute();
    }
}
