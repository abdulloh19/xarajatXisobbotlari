package com.hisobchi.bot.idempotency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "processed_updates", indexes = {
        @Index(name = "idx_processed_update_id", columnList = "update_id"),
        @Index(name = "idx_processed_callback_id", columnList = "callback_query_id"),
        @Index(name = "idx_processed_action_key", columnList = "action_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "update_id", unique = true)
    private Long updateId;

    @Column(name = "callback_query_id", unique = true, length = 100)
    private String callbackQueryId;

    @Column(name = "action_key", length = 255)
    private String actionKey;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }
}
