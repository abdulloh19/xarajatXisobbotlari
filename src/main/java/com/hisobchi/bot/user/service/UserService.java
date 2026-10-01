package com.hisobchi.bot.user.service;

import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.user.entity.User;
import com.hisobchi.bot.user.entity.UserState;
import com.hisobchi.bot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(Long telegramId, String firstName, String lastName, String username) {
        return userRepository.findByTelegramId(telegramId)
                .map(user -> {
                    // Update user info if changed
                    boolean updated = false;
                    if (firstName != null && !firstName.equals(user.getFirstName())) {
                        user.setFirstName(firstName);
                        updated = true;
                    }
                    if (lastName != null && !lastName.equals(user.getLastName())) {
                        user.setLastName(lastName);
                        updated = true;
                    }
                    if (username != null && !username.equals(user.getUsername())) {
                        user.setUsername(username);
                        updated = true;
                    }
                    return updated ? userRepository.save(user) : user;
                })
                .orElseGet(() -> {
                    log.info("Creating new user with telegramId: {}", telegramId);
                    User newUser = User.builder()
                            .telegramId(telegramId)
                            .firstName(firstName)
                            .lastName(lastName)
                            .username(username)
                            .language("uz")
                            .currency("UZS")
                            .timezone("Asia/Tashkent")
                            .state(UserState.IDLE)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @Transactional(readOnly = true)
    public User getByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new EntityNotFoundException("Foydalanuvchi topilmadi: " + telegramId));
    }

    @Transactional
    public void updateState(Long telegramId, UserState newState) {
        User user = getByTelegramId(telegramId);
        user.setState(newState);
        userRepository.save(user);
        log.debug("User {} state updated to {}", telegramId, newState);
    }

    @Transactional
    public void updateTimezone(Long telegramId, String timezone) {
        User user = getByTelegramId(telegramId);
        user.setTimezone(timezone);
        userRepository.save(user);
    }
}
