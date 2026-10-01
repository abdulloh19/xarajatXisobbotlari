package com.hisobchi.bot.debt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtReminderScheduler {

    private final DebtReminderService reminderService;
    private final DebtService debtService;

    // Run every minute at second 0
    @Scheduled(cron = "0 * * * * *")
    public void runDebtReminders() {
        try {
            reminderService.processDebtReminders();
        } catch (Exception e) {
            log.error("Error in runDebtReminders scheduler", e);
        }
    }

    // Run once an hour to check and mark overdue debts
    @Scheduled(cron = "0 5 * * * *")
    public void runOverdueCheck() {
        try {
            debtService.checkAndMarkOverdueDebts();
        } catch (Exception e) {
            log.error("Error checking overdue debts", e);
        }
    }
}
