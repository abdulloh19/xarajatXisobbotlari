package com.hisobchi.bot.todo.repository;

import com.hisobchi.bot.todo.entity.TodoProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TodoProjectRepository extends JpaRepository<TodoProject, Long> {

    List<TodoProject> findByUserIdAndArchivedFalseOrderByCreatedAtDesc(Long userId);

    Optional<TodoProject> findByIdAndUserId(Long id, Long userId);

    Optional<TodoProject> findByUserIdAndNameIgnoreCase(Long userId, String name);

    @Modifying
    @Query("DELETE FROM TodoProject p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
