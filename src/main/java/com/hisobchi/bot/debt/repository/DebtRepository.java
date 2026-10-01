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

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Debt d " +
           "WHERE d.user.id = :userId AND d.type = :type AND d.status IN :statuses")
    BigDecimal sumAmountByUserIdAndTypeAndStatusIn(
            @Param("userId") Long userId,
            @Param("type") DebtType type,
            @Param("statuses") Collection<DebtStatus> statuses);

    long countByUserIdAndTypeAndStatusIn(
            Long userId, DebtType type, Collection<DebtStatus> statuses);

    long countByUserIdAndStatus(Long userId, DebtStatus status);

    // Reminder search queries
    List<Debt> findByStatusInAndDueDate(Collection<DebtStatus> statuses, LocalDate dueDate);

    List<Debt> findByStatusInAndDueDateBefore(Collection<DebtStatus> statuses, LocalDate date);
}
