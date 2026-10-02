package com.hisobchi.bot.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal cashBalance = BigDecimal.ZERO;

    @Column(name = "card_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal cardBalance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BigDecimal getAvailableBalance() {
        BigDecimal cash = cashBalance != null ? cashBalance : BigDecimal.ZERO;
        BigDecimal card = cardBalance != null ? cardBalance : BigDecimal.ZERO;
        return cash.add(card);
    }
}
