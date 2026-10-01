package com.hisobchi.bot.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "username")
    private String username;

    @Column(name = "language", nullable = false)
    @Builder.Default
    private String language = "uz";

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "Asia/Tashkent";

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Builder.Default
    private UserState state = UserState.IDLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.language == null) this.language = "uz";
        if (this.currency == null) this.currency = "UZS";
        if (this.timezone == null) this.timezone = "Asia/Tashkent";
        if (this.state == null) this.state = UserState.IDLE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
