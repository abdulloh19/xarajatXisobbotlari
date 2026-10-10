package com.hisobchi.bot.todo.entity;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "todo_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private TodoProject project;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TodoStatus status = TodoStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TodoPriority priority = TodoPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "due_time")
    private LocalTime dueTime;

    @Column(name = "has_specific_time", nullable = false)
    @Builder.Default
    private Boolean hasSpecificTime = false;

    @Column(name = "reminder_at")
    private Instant reminderAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 30)
    @Builder.Default
    private TodoRecurrenceType recurrenceType = TodoRecurrenceType.NONE;

    @Column(name = "recurrence_days_of_week", length = 100)
    private String recurrenceDaysOfWeek;

    @Column(name = "recurrence_interval_days")
    private Integer recurrenceIntervalDays;

    @Column(name = "recurrence_day_of_month")
    private Integer recurrenceDayOfMonth;

    @Column(name = "recurrence_end")
    private LocalDate recurrenceEnd;

    @Column(name = "recurrence_parent_id")
    private Long recurrenceParentId;

    @Column(name = "planned_amount", precision = 19, scale = 2)
    private BigDecimal plannedAmount;

    @Column(name = "actual_amount", precision = 19, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "UZS";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_transaction_id")
    private Transaction linkedTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_debt_id")
    private Debt linkedDebt;

    @Column(name = "snooze_count", nullable = false)
    @Builder.Default
    private Integer snoozeCount = 0;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isOverdue(LocalDate today, LocalTime currentTime) {
        if (status == TodoStatus.COMPLETED || status == TodoStatus.CANCELLED) {
            return false;
        }
        if (dueDate == null) {
            return false;
        }
        if (dueDate.isBefore(today)) {
            return true;
        }
        if (dueDate.isEqual(today) && Boolean.TRUE.equals(hasSpecificTime) && dueTime != null) {
            return dueTime.isBefore(currentTime);
        }
        return false;
    }

    public boolean isRecurring() {
        return recurrenceType != null && recurrenceType != TodoRecurrenceType.NONE;
    }
}
