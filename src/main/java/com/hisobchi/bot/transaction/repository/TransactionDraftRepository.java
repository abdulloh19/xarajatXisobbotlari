package com.hisobchi.bot.transaction.repository;

import com.hisobchi.bot.transaction.entity.DraftStatus;
import com.hisobchi.bot.transaction.entity.TransactionDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TransactionDraftRepository extends JpaRepository<TransactionDraft, Long> {

    Optional<TransactionDraft> findByIdAndUserId(Long id, Long userId);

    Optional<TransactionDraft> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, DraftStatus status);

    @Modifying
    @Query("UPDATE TransactionDraft d SET d.status = :newStatus WHERE d.user.id = :userId AND d.status = :oldStatus")
    void cancelAllPendingByUserId(
            @Param("userId") Long userId,
            @Param("oldStatus") DraftStatus oldStatus,
            @Param("newStatus") DraftStatus newStatus);

    @Modifying
    @Query("UPDATE TransactionDraft d SET d.status = 'EXPIRED' WHERE d.status = 'PENDING' AND d.expiresAt < :now")
    int expireOldDrafts(@Param("now") Instant now);

    void deleteByUserId(Long userId);
}
