package com.hisobchi.bot.todo.repository;

import com.hisobchi.bot.todo.entity.TodoSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoSubtaskRepository extends JpaRepository<TodoSubtask, Long> {

    List<TodoSubtask> findByTaskIdOrderByOrderIndexAsc(Long taskId);

    long countByTaskId(Long taskId);

    long countByTaskIdAndCompletedTrue(Long taskId);

    @Modifying
    @Query("DELETE FROM TodoSubtask s WHERE s.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM TodoSubtask s WHERE s.task.id IN (SELECT t.id FROM TodoTask t WHERE t.user.id = :userId)")
    void deleteByUserId(@Param("userId") Long userId);
}
