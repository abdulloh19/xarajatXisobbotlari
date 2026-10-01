package com.hisobchi.bot.debt.entity;

import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "debt_reminder_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uq_debt_reminder_log", columnNames = {"debt_id", "reminder_type", "scheduled_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false)
    private Debt debt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reminder_type", nullable = false, length = 50)
    private String reminderType;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;
}
