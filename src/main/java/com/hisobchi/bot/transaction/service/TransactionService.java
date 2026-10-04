package com.hisobchi.bot.transaction.service;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.common.exception.UnauthorizedAccessException;
import com.hisobchi.bot.common.exception.ValidationException;
import com.hisobchi.bot.common.util.DateTimeUtils;
import com.hisobchi.bot.summary.service.DailySummaryService;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.DraftStatus;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionDraft;
import com.hisobchi.bot.transaction.repository.TransactionRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionDraftService draftService;
    private final DailySummaryService dailySummaryService;

    @Transactional
    public TransactionDto confirmAndSaveWithDate(Long draftId, Long userId, LocalDate targetDate) {
        TransactionDraft draft = draftService.getDraft(draftId, userId);

        if (draft.getStatus() != DraftStatus.PENDING) {
            throw new ValidationException("Ushbu operatsiya allaqachon tasdiqlangan yoki bekor qilingan.");
        }

        if (draft.getAmount() == null || draft.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Summa 0 dan katta bo‘lishi kerak.");
        }

        User user = draft.getUser();
        LocalDate transactionDate = targetDate != null ? targetDate : DateTimeUtils.today(user.getTimezone());

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(draft.getCategory())
                .type(draft.getType())
                .amount(draft.getAmount())
                .currency(draft.getCurrency())
                .description(draft.getDescription())
                .source(draft.getSource())
                .transactionDate(transactionDate)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        draftService.markConfirmed(draft);

        log.info("Transaction saved id: {}, user: {}, amount: {}, type: {}, date: {}",
                saved.getId(), userId, saved.getAmount(), saved.getType(), transactionDate);

        return toDto(saved);
    }

    @Transactional
    public TransactionDto confirmAndSave(Long draftId, Long userId) {
        return confirmAndSaveWithDate(draftId, userId, null);
    }

    @Transactional(readOnly = true)
    public Transaction getByIdAndUser(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Operatsiya topilmadi: " + transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            log.warn("Unauthorized transaction access: tx user={}, request user={}",
                    transaction.getUser().getId(), userId);
            throw new UnauthorizedAccessException("Ushbu operatsiya sizga tegishli emas!");
        }
        return transaction;
    }

    @Transactional
    public TransactionDto updateTransaction(
            Long transactionId,
            Long userId,
            BigDecimal amount,
            Category category,
            String description,
            LocalDate date) {

        Transaction tx = getByIdAndUser(transactionId, userId);

        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Summa 0 dan katta bo‘lishi kerak");
            }
            tx.setAmount(amount);
        }

        if (category != null) {
            tx.setCategory(category);
        }

        if (description != null) {
            tx.setDescription(description.isBlank() ? null : description.trim());
        }

        if (date != null) {
            tx.setTransactionDate(date);
        }

        Transaction updated = transactionRepository.save(tx);
        log.info("Updated transaction id: {} by user: {}", transactionId, userId);
        return toDto(updated);
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Long userId) {
        Transaction tx = getByIdAndUser(transactionId, userId);
        transactionRepository.delete(tx);
        log.info("Deleted transaction id: {} by user: {}", transactionId, userId);
    }

    public TransactionDto toDto(Transaction t) {
        String catName = t.getCategory() != null ? t.getCategory().getName() : "Boshqa";
        String catEmoji = t.getCategory() != null ? t.getCategory().getEmoji() : "📌";
        Long catId = t.getCategory() != null ? t.getCategory().getId() : null;

        return new TransactionDto(
                t.getId(),
                t.getUser().getId(),
                catId,
                catName,
                catEmoji,
                t.getType(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription(),
                t.getSource(),
                t.getTransactionDate(),
                t.getCreatedAt()
        );
    }
}
