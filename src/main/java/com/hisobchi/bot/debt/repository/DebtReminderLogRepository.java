package com.hisobchi.bot.debt.repository;

import com.hisobchi.bot.debt.entity.DebtReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DebtReminderLogRepository extends JpaRepository<DebtReminderLog, Long> {

    boolean existsByDebtIdAndReminderTypeAndScheduledDate(
            Long debtId, String reminderType, LocalDate scheduledDate);
}
