package com.hisobchi.bot.debt.entity;

import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "debt_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false)
    private Debt debt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private DebtPaymentType paymentType;

    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "Naqd";

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private TransactionSource source = TransactionSource.MANUAL;

    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDate paymentDate = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
