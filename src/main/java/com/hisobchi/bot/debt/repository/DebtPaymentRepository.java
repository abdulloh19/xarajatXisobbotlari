package com.hisobchi.bot.debt.repository;

import com.hisobchi.bot.debt.entity.DebtPayment;
import com.hisobchi.bot.debt.entity.DebtPaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    List<DebtPayment> findByDebtIdOrderByCreatedAtDesc(Long debtId);

    List<DebtPayment> findByUserIdAndPaymentDate(Long userId, LocalDate paymentDate);

    @Query("SELECT COALESCE(SUM(dp.amount), 0) FROM DebtPayment dp " +
           "WHERE dp.user.id = :userId AND dp.paymentType = :type AND dp.paymentDate = :date")
    BigDecimal sumAmountByUserIdAndPaymentTypeAndPaymentDate(
            @Param("userId") Long userId,
            @Param("type") DebtPaymentType type,
            @Param("date") LocalDate date);
}
