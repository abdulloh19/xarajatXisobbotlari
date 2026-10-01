package com.hisobchi.bot.debt.service;

import com.hisobchi.bot.debt.dto.DebtStatisticsDto;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtDraft;
import com.hisobchi.bot.debt.entity.DebtStatus;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtDraftService draftService;

    private static final List<DebtStatus> ACTIVE_STATUSES = List.of(DebtStatus.ACTIVE, DebtStatus.OVERDUE);
    private static final List<DebtStatus> CLOSED_STATUSES = List.of(DebtStatus.PAID, DebtStatus.RECEIVED);

    @Transactional
    public Debt saveFromDraft(DebtDraft draft) {
        Debt debt = Debt.builder()
                .user(draft.getUser())
                .type(draft.getType())
                .personName(draft.getPersonName())
                .amount(draft.getAmount())
                .currency("UZS")
                .paymentMethod(draft.getPaymentMethod() != null ? draft.getPaymentMethod() : "Naqd")
                .borrowedOrLentDate(draft.getBorrowedOrLentDate() != null ? draft.getBorrowedOrLentDate() : LocalDate.now())
                .dueDate(draft.getDueDate())
                .description(draft.getDescription())
                .status(DebtStatus.ACTIVE)
                .source(draft.getRawText() != null ? TransactionSource.VOICE : TransactionSource.MANUAL)
                .build();

        Debt saved = debtRepository.save(debt);
        draftService.deleteDraft(draft.getId());
        log.info("Saved debt id: {}, user: {}, type: {}, person: {}, amount: {}, method: {}",
                saved.getId(), draft.getUser().getId(), saved.getType(), saved.getPersonName(), saved.getAmount(), saved.getPaymentMethod());
        return saved;
    }

    @Transactional
    public Debt createDebt(User user, DebtType type, BigDecimal amount, String personName,
                           LocalDate dueDate, String paymentMethod, String description, TransactionSource source) {
        Debt debt = Debt.builder()
                .user(user)
                .type(type)
                .personName(personName)
                .amount(amount)
                .currency("UZS")
                .paymentMethod(paymentMethod != null ? paymentMethod : "Naqd")
                .borrowedOrLentDate(LocalDate.now())
                .dueDate(dueDate)
                .description(description)
                .status(DebtStatus.ACTIVE)
                .source(source)
                .build();
        return debtRepository.save(debt);
    }

    @Transactional(readOnly = true)
    public Optional<Debt> getDebt(Long debtId, Long userId) {
        return debtRepository.findByIdAndUserId(debtId, userId);
    }

    @Transactional(readOnly = true)
    public List<Debt> getActiveDebts(Long userId, DebtType type) {
        return debtRepository.findByUserIdAndTypeAndStatusInOrderByDueDateAscCreatedAtDesc(userId, type, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalActiveAmount(Long userId, DebtType type) {
        return debtRepository.sumAmountByUserIdAndTypeAndStatusIn(userId, type, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public List<Debt> getAllActiveDebts(Long userId) {
        return debtRepository.findByUserIdAndStatusInOrderByDueDateAscCreatedAtDesc(userId, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public List<Debt> getClosedDebts(Long userId) {
        return debtRepository.findByUserIdAndStatusInOrderByClosedAtDesc(userId, CLOSED_STATUSES);
    }

    @Transactional(readOnly = true)
    public DebtStatisticsDto getDebtStatistics(Long userId) {
        BigDecimal toPay = debtRepository.sumAmountByUserIdAndTypeAndStatusIn(userId, DebtType.BORROWED, ACTIVE_STATUSES);
        BigDecimal toReceive = debtRepository.sumAmountByUserIdAndTypeAndStatusIn(userId, DebtType.LENT, ACTIVE_STATUSES);
        long borrowedCount = debtRepository.countByUserIdAndTypeAndStatusIn(userId, DebtType.BORROWED, ACTIVE_STATUSES);
        long lentCount = debtRepository.countByUserIdAndTypeAndStatusIn(userId, DebtType.LENT, ACTIVE_STATUSES);
        long overdueCount = debtRepository.countByUserIdAndStatus(userId, DebtStatus.OVERDUE);

        return new DebtStatisticsDto(toPay, toReceive, borrowedCount, lentCount, overdueCount);
    }

    @Transactional
    public boolean markAsResolved(Long debtId, Long userId) {
        Optional<Debt> opt = debtRepository.findByIdAndUserId(debtId, userId);
        if (opt.isPresent()) {
            Debt debt = opt.get();
            if (debt.getType() == DebtType.BORROWED) {
                debt.setStatus(DebtStatus.PAID);
            } else {
                debt.setStatus(DebtStatus.RECEIVED);
            }
            debt.setPaidAt(Instant.now());
            debt.setClosedAt(Instant.now());
            debtRepository.save(debt);
            log.info("Debt {} resolved as {} by user {}", debtId, debt.getStatus(), userId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean extendDueDate(Long debtId, Long userId, LocalDate newDueDate) {
        Optional<Debt> opt = debtRepository.findByIdAndUserId(debtId, userId);
        if (opt.isPresent()) {
            Debt debt = opt.get();
            debt.setDueDate(newDueDate);
            debt.setStatus(DebtStatus.ACTIVE);
            debtRepository.save(debt);
            log.info("Debt {} dueDate extended to {} by user {}", debtId, newDueDate, userId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteDebt(Long debtId, Long userId) {
        Optional<Debt> opt = debtRepository.findByIdAndUserId(debtId, userId);
        if (opt.isPresent()) {
            debtRepository.delete(opt.get());
            log.info("Debt {} deleted by user {}", debtId, userId);
            return true;
        }
        return false;
    }

    @Transactional
    public void checkAndMarkOverdueDebts() {
        LocalDate today = LocalDate.now();
        List<Debt> overdueList = debtRepository.findByStatusInAndDueDateBefore(
                List.of(DebtStatus.ACTIVE), today);
        for (Debt d : overdueList) {
            d.setStatus(DebtStatus.OVERDUE);
            debtRepository.save(d);
            log.info("Debt {} marked as OVERDUE (dueDate: {})", d.getId(), d.getDueDate());
        }
    }
}
