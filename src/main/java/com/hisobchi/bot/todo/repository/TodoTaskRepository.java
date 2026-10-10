package com.hisobchi.bot.todo.repository;

import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.entity.TodoTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoTaskRepository extends JpaRepository<TodoTask, Long> {

    Optional<TodoTask> findByIdAndUserId(Long id, Long userId);

    List<TodoTask> findByUserIdAndStatusOrderByDueDateAscDueTimeAscCreatedAtDesc(Long userId, TodoStatus status, Pageable pageable);

    List<TodoTask> findByUserIdAndStatusInOrderByDueDateAscDueTimeAscCreatedAtDesc(Long userId, Collection<TodoStatus> statuses);

    long countByUserIdAndStatus(Long userId, TodoStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<TodoStatus> statuses);

    List<TodoTask> findByUserIdAndDueDateAndStatusInOrderByDueTimeAsc(Long userId, LocalDate dueDate, Collection<TodoStatus> statuses);

    List<TodoTask> findByUserIdAndDueDateBetweenAndStatusInOrderByDueDateAscDueTimeAsc(
            Long userId, LocalDate start, LocalDate end, Collection<TodoStatus> statuses);

    List<TodoTask> findByUserIdAndDueDateLessThanAndStatusInOrderByDueDateAscDueTimeAsc(
            Long userId, LocalDate date, Collection<TodoStatus> statuses);

    List<TodoTask> findByUserIdAndDueDateIsNullAndStatusInOrderByCreatedAtDesc(
            Long userId, Collection<TodoStatus> statuses);

    List<TodoTask> findByUserIdAndProjectIdAndStatusInOrderByDueDateAscCreatedAtDesc(
            Long userId, Long projectId, Collection<TodoStatus> statuses);

    long countByProjectIdAndStatus(Long projectId, TodoStatus status);

    long countByProjectId(Long projectId);

    @Query("SELECT COALESCE(SUM(t.plannedAmount), 0) FROM TodoTask t WHERE t.project.id = :projectId AND t.plannedAmount IS NOT NULL")
    BigDecimal sumPlannedAmountByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COALESCE(SUM(t.actualAmount), 0) FROM TodoTask t WHERE t.project.id = :projectId AND t.actualAmount IS NOT NULL")
    BigDecimal sumActualAmountByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COALESCE(SUM(t.plannedAmount), 0) FROM TodoTask t WHERE t.user.id = :userId AND t.dueDate BETWEEN :start AND :end AND t.plannedAmount IS NOT NULL AND t.status IN :statuses")
    BigDecimal sumPlannedAmountByUserIdAndDueDateBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("statuses") Collection<TodoStatus> statuses);

    @Query("SELECT t FROM TodoTask t WHERE t.reminderAt <= :now AND t.status IN ('OPEN', 'IN_PROGRESS') ORDER BY t.reminderAt ASC")
    List<TodoTask> findDueReminders(@Param("now") Instant now);

    @Query("SELECT t FROM TodoTask t WHERE t.user.id = :userId AND LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY t.dueDate ASC")
    List<TodoTask> searchByUserIdAndTitle(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    @Modifying
    @Query("DELETE FROM TodoTask t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
