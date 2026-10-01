package com.hisobchi.bot.notification.repository;

import com.hisobchi.bot.notification.entity.ReminderDeliveryLog;
import com.hisobchi.bot.notification.entity.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ReminderDeliveryLogRepository extends JpaRepository<ReminderDeliveryLog, Long> {
    boolean existsByUserIdAndReminderTypeAndReminderDate(Long userId, ReminderType reminderType, LocalDate reminderDate);
}
