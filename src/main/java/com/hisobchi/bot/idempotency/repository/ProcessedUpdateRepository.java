package com.hisobchi.bot.idempotency.repository;

import com.hisobchi.bot.idempotency.entity.ProcessedUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ProcessedUpdateRepository extends JpaRepository<ProcessedUpdate, Long> {

    boolean existsByUpdateId(Long updateId);

    boolean existsByCallbackQueryId(String callbackQueryId);

    boolean existsByActionKey(String actionKey);

    @Modifying
    @Query("DELETE FROM ProcessedUpdate p WHERE p.processedAt < :cutoff")
    int deleteOldRecords(@Param("cutoff") Instant cutoff);
}
