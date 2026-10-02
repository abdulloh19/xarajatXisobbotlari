package com.hisobchi.bot.debt.repository;

import com.hisobchi.bot.debt.entity.Debt;
import com.hisobchi.bot.debt.entity.DebtStatus;
import com.hisobchi.bot.debt.entity.DebtType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    Optional<Debt> findByIdAndUserId(Long id, Long userId);

    List<Debt> findByUserIdAndTypeAndStatusInOrderByDueDateAscCreatedAtDesc(
            Long userId, DebtType type, Collection<DebtStatus> statuses);

    List<Debt> findByUserIdAndStatusInOrderByDueDateAscCreatedAtDesc(
            Long userId, Collection<DebtStatus> statuses);

    List<Debt> findByUserIdAndStatusInOrderByClosedAtDesc(
            Long userId, Collection<DebtStatus> statuses);

    List<Debt> findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long userId, DebtType type, DebtStatus status);

    Page<Debt> findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long userId, DebtType type, DebtStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(COALESCE(d.remainingAmount, d.amount)), 0) FROM Debt d " +
           "WHERE d.user.id = :userId AND d.type = :type AND d.status IN :statuses")
    BigDecimal sumAmountByUserIdAndTypeAndStatusIn(
            @Param("userId") Long userId,
            @Param("type") DebtType type,
            @Param("statuses") Collection<DebtStatus> statuses);

    @Query("SELECT COALESCE(SUM(COALESCE(d.originalAmount, d.amount)), 0) FROM Debt d " +
           "WHERE d.user.id = :userId AND d.type = :type")
    BigDecimal sumOriginalAmountByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") DebtType type);

    @Query("SELECT COALESCE(SUM(d.paidAmount), 0) FROM Debt d " +
           "WHERE d.user.id = :userId AND d.type = :type")
    BigDecimal sumPaidAmountByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") DebtType type);

    @Query("SELECT COALESCE(SUM(COALESCE(d.remainingAmount, d.amount)), 0) FROM Debt d " +
           "WHERE d.user.id = :userId AND d.type = :type AND d.status IN :statuses")
    BigDecimal sumRemainingAmountByUserIdAndTypeAndStatusIn(
            @Param("userId") Long userId,
            @Param("type") DebtType type,
            @Param("statuses") Collection<DebtStatus> statuses);

    @Query("SELECT COALESCE(SUM(COALESCE(d.originalAmount, d.amount)), 0) FROM Debt d " +
           "WHERE d.user.id = :userId AND d.type = :type AND (d.startDate = :date OR d.borrowedOrLentDate = :date)")
    BigDecimal sumCreatedAmountByUserIdAndTypeAndDate(
            @Param("userId") Long userId,
            @Param("type") DebtType type,
            @Param("date") LocalDate date);

    long countByUserIdAndTypeAndStatusIn(
            Long userId, DebtType type, Collection<DebtStatus> statuses);

    long countByUserIdAndStatus(Long userId, DebtStatus status);

    // Person search & grouping
    List<Debt> findByUserIdAndTypeAndPersonNameIgnoreCaseAndStatusIn(
            Long userId, DebtType type, String personName, Collection<DebtStatus> statuses);

    List<Debt> findByUserIdAndPersonNameIgnoreCaseAndStatusIn(
            Long userId, String personName, Collection<DebtStatus> statuses);

    List<Debt> findByUserIdAndTypeAndStatusIn(
            Long userId, DebtType type, Collection<DebtStatus> statuses);

    List<Debt> findByUserIdAndStatusIn(
            Long userId, Collection<DebtStatus> statuses);

    // Due date queries
    List<Debt> findByUserIdAndDueDateBetweenAndStatusInOrderByDueDateAsc(
            Long userId, LocalDate fromDate, LocalDate toDate, Collection<DebtStatus> statuses);

    // Reminder search queries
    List<Debt> findByStatusInAndDueDate(Collection<DebtStatus> statuses, LocalDate dueDate);

    List<Debt> findByStatusInAndDueDateBefore(Collection<DebtStatus> statuses, LocalDate date);
}
