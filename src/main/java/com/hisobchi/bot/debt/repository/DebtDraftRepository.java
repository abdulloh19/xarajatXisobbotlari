package com.hisobchi.bot.debt.repository;

import com.hisobchi.bot.debt.entity.DebtDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DebtDraftRepository extends JpaRepository<DebtDraft, Long> {
    Optional<DebtDraft> findByIdAndUserId(Long id, Long userId);

    Optional<DebtDraft> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
