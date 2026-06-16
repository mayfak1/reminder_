package com.example.reminder.repository;

import com.example.reminder.domain.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile,Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndUserIdNot(String email,Long userId);

    boolean existsByTelegramChatId(Long telegramChatId);
    boolean existsByTelegramChatIdAndUserIdNot(Long telegramChatId, Long userId);

}
