package com.hisobchi.bot.user.repository;

import com.hisobchi.bot.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);
}
