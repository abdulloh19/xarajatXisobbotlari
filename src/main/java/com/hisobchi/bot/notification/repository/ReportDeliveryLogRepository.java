package com.hisobchi.bot.notification.repository;

import com.hisobchi.bot.notification.entity.ReportDeliveryLog;
import com.hisobchi.bot.notification.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ReportDeliveryLogRepository extends JpaRepository<ReportDeliveryLog, Long> {
    boolean existsByUserIdAndReportTypeAndPeriodStartAndPeriodEnd(
            Long userId, ReportType reportType, LocalDate periodStart, LocalDate periodEnd);
}
