package com.hisobchi.bot.transaction.service;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.common.exception.UnauthorizedAccessException;
import com.hisobchi.bot.common.exception.ValidationException;
import com.hisobchi.bot.config.BotConfig;
import com.hisobchi.bot.transaction.dto.DraftDto;
import com.hisobchi.bot.transaction.entity.DraftStatus;
import com.hisobchi.bot.transaction.entity.TransactionDraft;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.repository.TransactionDraftRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDraftService {

    private final TransactionDraftRepository draftRepository;
    private final BotConfig botConfig;

    @Transactional
    public TransactionDraft createDraft(
            User user,
            TransactionType type,
            BigDecimal amount,
            Category category,
            String description,
            TransactionSource source,
            String originalText,
            Double confidence) {

        // Cancel previous pending drafts for this user to avoid stale confirmations
        draftRepository.cancelAllPendingByUserId(user.getId(), DraftStatus.PENDING, DraftStatus.CANCELLED);

        Instant expiresAt = Instant.now().plus(botConfig.getDraftTtlMinutes(), ChronoUnit.MINUTES);

        TransactionDraft draft = TransactionDraft.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .category(category)
                .description(description)
                .source(source != null ? source : TransactionSource.MANUAL)
                .originalText(originalText)
                .confidence(confidence)
                .status(DraftStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        TransactionDraft saved = draftRepository.save(draft);
        log.debug("Created transaction draft id: {} for user: {}", saved.getId(), user.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public TransactionDraft getDraft(Long draftId, Long userId) {
        TransactionDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new EntityNotFoundException("Draft topilmadi: " + draftId));

        if (!draft.getUser().getId().equals(userId)) {
            log.warn("Unauthorized draft access attempt: draft user={}, requesting user={}",
                    draft.getUser().getId(), userId);
            throw new UnauthorizedAccessException("Ushbu draft sizga tegishli emas!");
        }

        if (draft.isExpired() && draft.getStatus() == DraftStatus.PENDING) {
            draft.setStatus(DraftStatus.EXPIRED);
            draftRepository.save(draft);
            throw new ValidationException("Ushbu tasdiqlash vaqti tugagan. Qaytadan kiriting.");
        }

        return draft;
    }

    @Transactional(readOnly = true)
    public Optional<TransactionDraft> getLatestPendingDraft(Long userId) {
        return draftRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, DraftStatus.PENDING);
    }

    @Transactional
    public TransactionDraft updateDraftAmount(Long draftId, Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Summa 0 dan katta bo‘lishi kerak");
        }
        TransactionDraft draft = getDraft(draftId, userId);
        draft.setAmount(amount);
        return draftRepository.save(draft);
    }

    @Transactional
    public TransactionDraft updateDraftCategory(Long draftId, Long userId, Category category) {
        TransactionDraft draft = getDraft(draftId, userId);
        draft.setCategory(category);
        return draftRepository.save(draft);
    }

    @Transactional
    public TransactionDraft updateDraftDescription(Long draftId, Long userId, String description) {
        TransactionDraft draft = getDraft(draftId, userId);
        draft.setDescription(description);
        return draftRepository.save(draft);
    }

    @Transactional
    public TransactionDraft updateDraftType(Long draftId, Long userId, TransactionType type) {
        TransactionDraft draft = getDraft(draftId, userId);
        draft.setType(type);
        draft.setCategory(null);
        return draftRepository.save(draft);
    }

    @Transactional
    public void markConfirmed(TransactionDraft draft) {
        draft.setStatus(DraftStatus.CONFIRMED);
        draftRepository.save(draft);
    }

    @Transactional
    public void cancelDraft(Long draftId, Long userId) {
        TransactionDraft draft = getDraft(draftId, userId);
        draft.setStatus(DraftStatus.CANCELLED);
        draftRepository.save(draft);
        log.info("Draft {} cancelled by user {}", draftId, userId);
    }

    public DraftDto toDto(TransactionDraft d) {
        String catName = d.getCategory() != null ? d.getCategory().getName() : null;
        String catEmoji = d.getCategory() != null ? d.getCategory().getEmoji() : null;
        Long catId = d.getCategory() != null ? d.getCategory().getId() : null;

        return new DraftDto(
                d.getId(),
                d.getUser().getId(),
                catId,
                catName,
                catEmoji,
                d.getType(),
                d.getAmount(),
                d.getCurrency(),
                d.getDescription(),
                d.getSource(),
                d.getOriginalText(),
                d.getConfidence(),
                d.getStatus(),
                d.getExpiresAt(),
                d.getCreatedAt()
        );
    }

    @Scheduled(fixedRate = 600000) // Every 10 minutes
    @Transactional
    public void cleanExpiredDrafts() {
        int expired = draftRepository.expireOldDrafts(Instant.now());
        if (expired > 0) {
            log.info("Expired {} old transaction drafts", expired);
        }
    }
}
