package com.hisobchi.bot.profit.entity;

import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_profits", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_profits_user_date", columnNames = {"user_id", "profit_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyProfit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "profit_date", nullable = false)
    private LocalDate profitDate;

    @Column(name = "cash_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal cashAmount = BigDecimal.ZERO;

    @Column(name = "card_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal cardAmount = BigDecimal.ZERO;

    @Column(name = "total_profit", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalProfit = BigDecimal.ZERO;

    @Column(name = "is_work_day", nullable = false)
    @Builder.Default
    private boolean isWorkDay = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
