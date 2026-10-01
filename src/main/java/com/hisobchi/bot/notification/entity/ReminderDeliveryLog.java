package com.hisobchi.bot.notification.entity;

import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reminder_delivery_log", uniqueConstraints = {
        @UniqueConstraint(name = "uq_reminder_delivery", columnNames = {"user_id", "reminder_type", "reminder_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 50)
    private ReminderType reminderType;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;
}
