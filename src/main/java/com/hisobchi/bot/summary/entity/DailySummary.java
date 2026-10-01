package com.hisobchi.bot.summary.entity;

import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_summaries", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_summaries_user_date", columnNames = {"user_id", "summary_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "total_income", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_expense", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "net_profit", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO;

    @Column(name = "closed", nullable = false)
    @Builder.Default
    private Boolean closed = false;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.totalIncome == null) this.totalIncome = BigDecimal.ZERO;
        if (this.totalExpense == null) this.totalExpense = BigDecimal.ZERO;
        if (this.netProfit == null) this.netProfit = BigDecimal.ZERO;
        if (this.closed == null) this.closed = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
