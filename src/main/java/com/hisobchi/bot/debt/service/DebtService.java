package com.hisobchi.bot.debt.service;

import com.hisobchi.bot.common.exception.ValidationException;
import com.hisobchi.bot.debt.dto.DebtStatisticsDto;
import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtDraft;
import com.hisobchi.bot.debt.entity.DebtPayment;
import com.hisobchi.bot.debt.entity.DebtPaymentType;
import com.hisobchi.bot.debt.entity.DebtStatus;
import com.hisobchi.bot.debt.entity.DebtType;
import com.hisobchi.bot.debt.repository.DebtPaymentRepository;
import com.hisobchi.bot.debt.repository.DebtRepository;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtDraftService draftService;
    private final DebtPaymentRepository debtPaymentRepository;
    private final BalanceService balanceService;

    public static final List<DebtStatus> ACTIVE_STATUSES = List.of(DebtStatus.ACTIVE, DebtStatus.OVERDUE);
    public static final List<DebtStatus> CLOSED_STATUSES = List.of(DebtStatus.PAID, DebtStatus.RECEIVED);

    @Transactional
    public Debt saveFromDraft(DebtDraft draft) {
        BigDecimal amt = draft.getAmount() != null ? draft.getAmount() : BigDecimal.ZERO;
        String method = draft.getPaymentMethod() != null && !draft.getPaymentMethod().isBlank() ? draft.getPaymentMethod() : "Naqd";
        LocalDate startDate = draft.getBorrowedOrLentDate() != null ? draft.getBorrowedOrLentDate() : LocalDate.now();

        Debt debt = Debt.builder()
                .user(draft.getUser())
                .type(draft.getType())
                .personName(draft.getPersonName())
                .amount(amt)
                .originalAmount(amt)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(amt)
                .currency("UZS")
                .paymentMethod(method)
                .initialPaymentMethod(method)
                .borrowedOrLentDate(startDate)
                .startDate(startDate)
                .dueDate(draft.getDueDate())
                .description(draft.getDescription())
                .status(DebtStatus.ACTIVE)
                .source(draft.getRawText() != null ? TransactionSource.VOICE : TransactionSource.MANUAL)
                .build();

        Debt saved = debtRepository.save(debt);
        draftService.deleteDraft(draft.getId());

        // Update real available balance
        if (saved.getType() == DebtType.LENT) {
            balanceService.applyLentDebt(saved.getUser(), saved.getOriginalAmount(), saved.getPaymentMethod());
        } else if (saved.getType() == DebtType.BORROWED) {
            balanceService.applyBorrowedDebt(saved.getUser(), saved.getOriginalAmount(), saved.getPaymentMethod());
        }

        log.info("Saved debt id: {}, user: {}, type: {}, person: {}, amount: {}, method: {}",
                saved.getId(), draft.getUser().getId(), saved.getType(), saved.getPersonName(), saved.getAmount(), saved.getPaymentMethod());
        return saved;
    }

    @Transactional
    public Debt createDebt(User user, DebtType type, BigDecimal amount, String personName,
                           LocalDate dueDate, String paymentMethod, String description, TransactionSource source) {
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;
        String method = paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "Naqd";

        Debt debt = Debt.builder()
                .user(user)
                .type(type)
                .personName(personName)
                .amount(amt)
                .originalAmount(amt)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(amt)
                .currency("UZS")
                .paymentMethod(method)
                .initialPaymentMethod(method)
                .borrowedOrLentDate(LocalDate.now())
                .startDate(LocalDate.now())
                .dueDate(dueDate)
                .description(description)
                .status(DebtStatus.ACTIVE)
                .source(source)
                .build();

        Debt saved = debtRepository.save(debt);

        // Update real available balance
        if (saved.getType() == DebtType.LENT) {
            balanceService.applyLentDebt(saved.getUser(), saved.getOriginalAmount(), saved.getPaymentMethod());
        } else if (saved.getType() == DebtType.BORROWED) {
            balanceService.applyBorrowedDebt(saved.getUser(), saved.getOriginalAmount(), saved.getPaymentMethod());
        }

        return saved;
    }

    @Transactional
    public DebtPayment makePartialPayment(Long debtId, Long userId, BigDecimal paymentAmount, String paymentMethod, TransactionSource source) {
        Debt debt = debtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new ValidationException("Qarz topilmadi (ID: " + debtId + ")"));

        if (!ACTIVE_STATUSES.contains(debt.getStatus())) {
            throw new ValidationException("Bu qarz allaqachon yopilgan.");
        }

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("To‘lov summasi 0 dan katta bo‘lishi kerak.");
        }

        BigDecimal remaining = debt.getRemainingAmount();
        if (paymentAmount.compareTo(remaining) > 0) {
            throw new ValidationException("To‘lov summasi qolgan qarzdan (" + remaining + ") ko‘p bo‘lishi mumkin emas.");
        }

        String method = paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "Naqd";
        DebtPaymentType paymentType = debt.getType() == DebtType.BORROWED ? DebtPaymentType.DEBT_PAYMENT : DebtPaymentType.DEBT_RETURN;

        BigDecimal newPaid = debt.getPaidAmount().add(paymentAmount);
        BigDecimal newRemaining = remaining.subtract(paymentAmount);

        debt.setPaidAmount(newPaid);
        debt.setRemainingAmount(newRemaining);

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            debt.setRemainingAmount(BigDecimal.ZERO);
            debt.setStatus(debt.getType() == DebtType.BORROWED ? DebtStatus.PAID : DebtStatus.RECEIVED);
            debt.setPaidAt(Instant.now());
            debt.setClosedAt(Instant.now());
        }

        debtRepository.save(debt);

        DebtPayment payment = DebtPayment.builder()
                .debt(debt)
                .user(debt.getUser())
                .amount(paymentAmount)
                .paymentType(paymentType)
                .paymentMethod(method)
                .source(source != null ? source : TransactionSource.MANUAL)
                .paymentDate(LocalDate.now())
                .build();
        DebtPayment savedPayment = debtPaymentRepository.save(payment);

        // Update balances:
        if (debt.getType() == DebtType.BORROWED) {
            balanceService.applyDebtPayment(debt.getUser(), paymentAmount, method);
        } else {
            balanceService.applyDebtReturn(debt.getUser(), paymentAmount, method);
        }

        log.info("Debt {} payment recorded: amount={}, remaining={}, status={}", debtId, paymentAmount, debt.getRemainingAmount(), debt.getStatus());
        return savedPayment;
    }

    @Transactional
    public DebtPayment makeFullPayment(Long debtId, Long userId, String paymentMethod, TransactionSource source) {
        Debt debt = debtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new ValidationException("Qarz topilmadi (ID: " + debtId + ")"));
        BigDecimal remaining = debt.getRemainingAmount();
        return makePartialPayment(debtId, userId, remaining, paymentMethod, source);
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
    public List<Debt> getNearDueDebts(Long userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate target = today.plusDays(days);
        return debtRepository.findByUserIdAndDueDateBetweenAndStatusInOrderByDueDateAsc(userId, today, target, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public List<DebtPayment> getPaymentsForDebt(Long debtId) {
        return debtPaymentRepository.findByDebtIdOrderByCreatedAtDesc(debtId);
    }

    @Transactional(readOnly = true)
    public List<DebtPayment> getPaymentsForDebt(Long debtId, Long userId) {
        Optional<Debt> debtOpt = debtRepository.findByIdAndUserId(debtId, userId);
        if (debtOpt.isEmpty()) return List.of();
        return debtPaymentRepository.findByDebtIdOrderByCreatedAtDesc(debtId);
    }

    @Transactional(readOnly = true)
    public DebtStatisticsDto getDebtStatistics(Long userId) {
        BigDecimal borrowedOriginal = debtRepository.sumOriginalAmountByUserIdAndType(userId, DebtType.BORROWED);
        BigDecimal borrowedPaid = debtRepository.sumPaidAmountByUserIdAndType(userId, DebtType.BORROWED);
        BigDecimal borrowedRemaining = debtRepository.sumRemainingAmountByUserIdAndTypeAndStatusIn(userId, DebtType.BORROWED, ACTIVE_STATUSES);

        BigDecimal lentOriginal = debtRepository.sumOriginalAmountByUserIdAndType(userId, DebtType.LENT);
        BigDecimal lentPaid = debtRepository.sumPaidAmountByUserIdAndType(userId, DebtType.LENT);
        BigDecimal lentRemaining = debtRepository.sumRemainingAmountByUserIdAndTypeAndStatusIn(userId, DebtType.LENT, ACTIVE_STATUSES);

        long borrowedCount = debtRepository.countByUserIdAndTypeAndStatusIn(userId, DebtType.BORROWED, ACTIVE_STATUSES);
        long lentCount = debtRepository.countByUserIdAndTypeAndStatusIn(userId, DebtType.LENT, ACTIVE_STATUSES);
        long overdueCount = debtRepository.countByUserIdAndStatus(userId, DebtStatus.OVERDUE);

        return new DebtStatisticsDto(
                borrowedOriginal != null ? borrowedOriginal : BigDecimal.ZERO,
                borrowedPaid != null ? borrowedPaid : BigDecimal.ZERO,
                borrowedRemaining != null ? borrowedRemaining : BigDecimal.ZERO,
                lentOriginal != null ? lentOriginal : BigDecimal.ZERO,
                lentPaid != null ? lentPaid : BigDecimal.ZERO,
                lentRemaining != null ? lentRemaining : BigDecimal.ZERO,
                borrowedCount,
                lentCount,
                overdueCount
        );
    }

    @Transactional
    public boolean markAsResolved(Long debtId, Long userId) {
        Optional<Debt> opt = debtRepository.findByIdAndUserId(debtId, userId);
        if (opt.isPresent()) {
            Debt debt = opt.get();
            BigDecimal remaining = debt.getRemainingAmount();
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                makePartialPayment(debtId, userId, remaining, debt.getPaymentMethod(), TransactionSource.MANUAL);
            } else {
                debt.setStatus(debt.getType() == DebtType.BORROWED ? DebtStatus.PAID : DebtStatus.RECEIVED);
                debt.setPaidAt(Instant.now());
                debt.setClosedAt(Instant.now());
                debtRepository.save(debt);
            }
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

    @Transactional(readOnly = true)
    public Optional<Debt> findMatchingActiveDebt(Long userId, String personName, DebtType type) {
        if (personName == null || personName.isBlank()) return Optional.empty();

        List<Debt> activeDebts = debtRepository.findByUserIdAndTypeAndStatusIn(userId, type, ACTIVE_STATUSES);
        String target = personName.trim().toLowerCase();

        // 1. Exact match
        for (Debt d : activeDebts) {
            if (d.getPersonName().equalsIgnoreCase(target)) {
                return Optional.of(d);
            }
        }

        // 2. Substring match
        for (Debt d : activeDebts) {
            String p = d.getPersonName().toLowerCase();
            if (p.contains(target) || target.contains(p)) {
                return Optional.of(d);
            }
        }

        // 3. Normalized match without suffixes (aka, opa, etc.)
        String cleanTarget = target.replaceAll("\\s+(aka|opa|uka|singil|tog'a|toga|amaki|xola|pochcha)\\b", "").trim();
        for (Debt d : activeDebts) {
            String cleanP = d.getPersonName().toLowerCase().replaceAll("\\s+(aka|opa|uka|singil|tog'a|toga|amaki|xola|pochcha)\\b", "").trim();
            if (cleanP.equals(cleanTarget) || cleanP.contains(cleanTarget) || cleanTarget.contains(cleanP)) {
                return Optional.of(d);
            }
        }

        return Optional.empty();
    }
}
