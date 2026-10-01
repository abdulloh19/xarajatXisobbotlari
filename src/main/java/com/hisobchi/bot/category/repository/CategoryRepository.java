package com.hisobchi.bot.category.repository;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdAndTypeAndIsActiveTrueOrderByIdAsc(Long userId, CategoryType type);

    Optional<Category> findByUserIdAndNameIgnoreCaseAndType(Long userId, String name, CategoryType type);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCaseAndType(Long userId, String name, CategoryType type);

    long countByUserId(Long userId);

    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.isActive = true ORDER BY c.id ASC")
    List<Category> findAllActiveByUserId(@Param("userId") Long userId);
}
