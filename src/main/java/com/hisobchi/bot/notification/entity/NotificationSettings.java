package com.hisobchi.bot.notification.entity;

import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "profit_reminder_18_enabled", nullable = false)
    @Builder.Default
    private Boolean profitReminder18Enabled = true;

    @Column(name = "profit_reminder_21_enabled", nullable = false)
    @Builder.Default
    private Boolean profitReminder21Enabled = true;

    @Column(name = "daily_report_enabled", nullable = false)
    @Builder.Default
    private Boolean dailyReportEnabled = true;

    @Column(name = "weekly_report_enabled", nullable = false)
    @Builder.Default
    private Boolean weeklyReportEnabled = true;

    @Column(name = "two_week_report_enabled", nullable = false)
    @Builder.Default
    private Boolean twoWeekReportEnabled = true;

    @Column(name = "three_week_report_enabled", nullable = false)
    @Builder.Default
    private Boolean threeWeekReportEnabled = true;

    @Column(name = "monthly_report_enabled", nullable = false)
    @Builder.Default
    private Boolean monthlyReportEnabled = true;

    @Column(name = "daily_report_time", nullable = false, length = 10)
    @Builder.Default
    private String dailyReportTime = "23:00";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
