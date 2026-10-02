package com.hisobchi.bot.debt.entity;

import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "debt_drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DebtType type;

    @Column(name = "person_name", nullable = false)
    private String personName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private String paymentMethod = "Naqd";

    @Column(name = "initial_payment_method", length = 20)
    @Builder.Default
    private String initialPaymentMethod = "Naqd";

    @Column(name = "borrowed_or_lent_date")
    @Builder.Default
    private LocalDate borrowedOrLentDate = LocalDate.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
