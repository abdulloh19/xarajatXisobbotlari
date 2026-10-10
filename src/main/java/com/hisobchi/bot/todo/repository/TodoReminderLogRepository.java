package com.hisobchi.bot.todo.repository;

import com.hisobchi.bot.todo.entity.TodoReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TodoReminderLogRepository extends JpaRepository<TodoReminderLog, Long> {

    List<TodoReminderLog> findByTaskIdOrderByRemindedAtDesc(Long taskId);

    @Query("SELECT COUNT(l) FROM TodoReminderLog l WHERE l.user.id = :userId AND l.remindedAt >= :startOfDay")
    long countDeliveredToday(@Param("userId") Long userId, @Param("startOfDay") Instant startOfDay);

    @Modifying
    @Query("DELETE FROM TodoReminderLog l WHERE l.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
