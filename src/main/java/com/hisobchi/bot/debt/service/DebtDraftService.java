package com.hisobchi.bot.debt.service;

import com.hisobchi.bot.debt.entity.DebtDraft;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtDraftRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtDraftService {

    private final DebtDraftRepository draftRepository;
    private static final Duration DRAFT_TTL = Duration.ofMinutes(30);

    @Transactional
    public DebtDraft createDraft(
            User user,
            DebtType type,
            BigDecimal amount,
            String personName,
            LocalDate dueDate,
            String description,
            String rawText,
            double confidence
    ) {
        return createDraft(user, type, amount, personName, dueDate, description, rawText, confidence, "Naqd", LocalDate.now());
    }

    @Transactional
    public DebtDraft createDraft(
            User user,
            DebtType type,
            BigDecimal amount,
            String personName,
            LocalDate dueDate,
            String description,
            String rawText,
            double confidence,
            String paymentMethod,
            LocalDate borrowedOrLentDate
    ) {
        DebtDraft draft = DebtDraft.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .personName(personName != null && !personName.isBlank() ? personName : "Noma'lum")
                .dueDate(dueDate)
                .description(description)
                .rawText(rawText)
                .confidence(confidence)
                .paymentMethod(paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "Naqd")
                .borrowedOrLentDate(borrowedOrLentDate != null ? borrowedOrLentDate : LocalDate.now())
                .expiresAt(Instant.now().plus(DRAFT_TTL))
                .build();

        DebtDraft saved = draftRepository.save(draft);
        log.debug("Created debt draft id: {} for user: {}", saved.getId(), user.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DebtDraft> findValidDraft(Long draftId, Long userId) {
        return draftRepository.findByIdAndUserId(draftId, userId)
                .filter(d -> !d.isExpired());
    }

    @Transactional(readOnly = true)
    public Optional<DebtDraft> findLatestActiveDraft(Long userId) {
        return draftRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(d -> !d.isExpired());
    }

    @Transactional
    public void updateDraftAmount(Long draftId, Long userId, BigDecimal newAmount) {
        findValidDraft(draftId, userId).ifPresent(d -> {
            d.setAmount(newAmount);
            draftRepository.save(d);
        });
    }

    @Transactional
    public void updateDraftPerson(Long draftId, Long userId, String newPerson) {
        findValidDraft(draftId, userId).ifPresent(d -> {
            d.setPersonName(newPerson);
            draftRepository.save(d);
        });
    }

    @Transactional
    public void updateDraftDate(Long draftId, Long userId, LocalDate newDate) {
        findValidDraft(draftId, userId).ifPresent(d -> {
            d.setDueDate(newDate);
            draftRepository.save(d);
        });
    }

    @Transactional
    public void updateDraftPaymentMethod(Long draftId, Long userId, String method) {
        findValidDraft(draftId, userId).ifPresent(d -> {
            d.setPaymentMethod(method);
            draftRepository.save(d);
        });
    }

    @Transactional
    public void deleteDraft(Long draftId) {
        draftRepository.deleteById(draftId);
    }
}
