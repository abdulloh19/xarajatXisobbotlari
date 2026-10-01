package com.hisobchi.bot.transaction.repository;

import com.hisobchi.bot.statistics.dto.CategoryExpenseDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.transactionDate = :date")
    BigDecimal sumAmountByUserIdAndTypeAndDate(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionDate = :date")
    long countByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT t.transactionDate) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countActiveDaysBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT new com.hisobchi.bot.statistics.dto.CategoryExpenseDto(" +
           "COALESCE(c.id, 0L), " +
           "COALESCE(c.name, 'Boshqa'), " +
           "COALESCE(c.emoji, '📌'), " +
           "SUM(t.amount), " +
           "COUNT(t)) " +
           "FROM Transaction t " +
           "LEFT JOIN t.category c " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.transactionDate = :date " +
           "GROUP BY c.id, c.name, c.emoji " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategoryExpenseDto> findCategoryExpensesByDate(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("date") LocalDate date);

    @Query("SELECT new com.hisobchi.bot.statistics.dto.CategoryExpenseDto(" +
           "COALESCE(c.id, 0L), " +
           "COALESCE(c.name, 'Boshqa'), " +
           "COALESCE(c.emoji, '📌'), " +
           "SUM(t.amount), " +
           "COUNT(t)) " +
           "FROM Transaction t " +
           "LEFT JOIN t.category c " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY c.id, c.name, c.emoji " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategoryExpenseDto> findCategoryExpensesBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Transaction> findByUserIdAndTransactionDateOrderByCreatedAtAsc(Long userId, LocalDate date);

    Page<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Transaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId, Pageable pageable);
}
