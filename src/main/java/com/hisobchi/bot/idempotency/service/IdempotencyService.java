package com.hisobchi.bot.idempotency.service;

import com.hisobchi.bot.idempotency.entity.ProcessedUpdate;
import com.hisobchi.bot.idempotency.repository.ProcessedUpdateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedUpdateRepository repository;

    @Transactional
    public boolean isUpdateProcessed(Long updateId) {
        if (updateId == null) return false;
        return repository.existsByUpdateId(updateId);
    }

    @Transactional
    public void markUpdateProcessed(Long updateId) {
        if (updateId == null) return;
        try {
            repository.save(ProcessedUpdate.builder()
                    .updateId(updateId)
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.debug("Update {} already recorded as processed", updateId);
        }
    }

    @Transactional
    public boolean tryAcquireAction(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) return true;
        if (repository.existsByActionKey(actionKey)) {
            log.warn("Duplicate action detected for key: {}", actionKey);
            return false;
        }
        try {
            repository.save(ProcessedUpdate.builder()
                    .actionKey(actionKey)
                    .processedAt(Instant.now())
                    .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate action prevented for key: {}", actionKey);
            return false;
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupOldRecords() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int deleted = repository.deleteOldRecords(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} old idempotency records", deleted);
        }
    }
}
