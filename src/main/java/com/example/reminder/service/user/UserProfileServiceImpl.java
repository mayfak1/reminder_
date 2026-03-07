package com.example.reminder.service.user;

import com.example.reminder.domain.user.User;
import com.example.reminder.domain.user.UserProfile;
import com.example.reminder.dto.user.UserProfileRequestAndResponse;
import com.example.reminder.exceptions.ConflictException;
import com.example.reminder.exceptions.NotFoundException;
import com.example.reminder.mapper.UserProfileMapper;
import com.example.reminder.repository.UserProfileRepository;
import com.example.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService{
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;
    @Override
    public UserProfileRequestAndResponse getProfile(Long userId) {
        log.debug("Profile:get userId={}", userId);
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Profile:get not found userId={}", userId);
                    return new NotFoundException("Profile not found");
                });
        log.debug("Profile:get success userId={} hasEmail={} hasTelegram={}",
                userId,
                profile.getEmail() != null && !profile.getEmail().isBlank(),
                profile.getTelegramChatId() != null);
        return userProfileMapper.toResponse(profile);
    }



    @Override
    @Transactional
    public UserProfileRequestAndResponse upsertProfile(Long userId, UserProfileRequestAndResponse request) {
        log.info("Profile:upsert started userId={} hasEmail={} hasTelegram={}",
                userId,
                request.email() != null && !request.email().isBlank(),
                request.telegramId() != null);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Profile:upsert user not found userId={}", userId);
                    return new IllegalArgumentException("User not found");
                });
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userProfileRepository.existsByEmailIgnoreCaseAndUserIdNot(normalizedEmail, userId)) {
            log.warn("Profile:upsert email conflict userId={}", userId);
            throw new ConflictException("Email already in use");
        }
        Long chatId = request.telegramId();
        if (chatId != null && userProfileRepository.existsByTelegramChatIdAndUserIdNot(chatId, userId)) {
            log.warn("Profile:upsert telegram conflict userId={} chatId={}", userId, chatId);
            throw new ConflictException("Telegram id already in use");
        }
        UserProfile existingProfile = userProfileRepository.findById(userId).orElse(null);
        boolean created = existingProfile == null;
        UserProfile profile;
        if (existingProfile != null) {
            profile = existingProfile;
        } else {
            profile = new UserProfile();
            profile.setUser(user);
        }

        profile.setEmail(normalizedEmail);
        profile.setTelegramChatId(chatId);

        try {
            userProfileRepository.save(profile);
        } catch (DataIntegrityViolationException e) {
            log.warn("Profile:upsert data integrity conflict userId={} message={}", userId, e.getMessage());
            throw new ConflictException("Email or telegram chat id already in use");
        }
        log.info("Profile:upsert success userId={} created={} hasTelegram={}",
                userId, created, profile.getTelegramChatId() != null);
        return userProfileMapper.toResponse(profile);
    }



}
