package com.hisobchi.bot.debt.entity;

import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "debts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Debt {

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

    @Column(name = "original_amount", precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private String paymentMethod = "Naqd";

    @Column(name = "initial_payment_method", length = 20)
    @Builder.Default
    private String initialPaymentMethod = "Naqd";

    @Column(name = "borrowed_or_lent_date")
    @Builder.Default
    private LocalDate borrowedOrLentDate = LocalDate.now();

    @Column(name = "start_date")
    @Builder.Default
    private LocalDate startDate = LocalDate.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DebtStatus status = DebtStatus.ACTIVE;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private TransactionSource source = TransactionSource.MANUAL;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.originalAmount == null && this.amount != null) {
            this.originalAmount = this.amount;
        } else if (this.amount == null && this.originalAmount != null) {
            this.amount = this.originalAmount;
        }
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.remainingAmount == null) {
            this.remainingAmount = this.originalAmount != null ? this.originalAmount.subtract(this.paidAmount) : BigDecimal.ZERO;
        }
        if (this.initialPaymentMethod == null) {
            this.initialPaymentMethod = this.paymentMethod;
        }
        if (this.startDate == null) {
            this.startDate = this.borrowedOrLentDate != null ? this.borrowedOrLentDate : LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.originalAmount == null && this.amount != null) {
            this.originalAmount = this.amount;
        } else if (this.amount == null && this.originalAmount != null) {
            this.amount = this.originalAmount;
        }
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.remainingAmount == null && this.originalAmount != null) {
            this.remainingAmount = this.originalAmount.subtract(this.paidAmount);
        }
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount != null ? originalAmount : amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount != null ? paidAmount : BigDecimal.ZERO;
    }

    public BigDecimal getRemainingAmount() {
        if (remainingAmount != null) return remainingAmount;
        BigDecimal orig = getOriginalAmount();
        BigDecimal paid = getPaidAmount();
        return orig != null ? orig.subtract(paid) : BigDecimal.ZERO;
    }
}
