package com.hisobchi.bot.todo.entity;

import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "todo_reminder_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TodoTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "reminded_at", nullable = false, updatable = false)
    private Instant remindedAt;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DELIVERED";

    @Column(name = "delivery_error", columnDefinition = "TEXT")
    private String deliveryError;
}
