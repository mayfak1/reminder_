package com.example.reminder.web.user;

import com.example.reminder.dto.user.UserProfileRequestAndResponse;
import com.example.reminder.service.user.CurrentUserService;
import com.example.reminder.service.user.UserProfileServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    private final UserProfileServiceImpl userProfileService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public UserProfileRequestAndResponse getUserProfile(@AuthenticationPrincipal Jwt jwt){
        Long userId = currentUserService.getOrCreateUserId(jwt);
        log.debug("Api: profile get userId={}", userId);
        UserProfileRequestAndResponse response = userProfileService.getProfile(userId);
        log.debug("Api: profile get success userId={}", userId);
        return response;
    }

    @PutMapping
    public UserProfileRequestAndResponse upsertUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileRequestAndResponse dto
    ){
        Long userId = currentUserService.getOrCreateUserId(jwt);
        log.info("Api: profile upsert userId={} hasEmail={} hasTelegram={}",
                userId,
                dto.email() != null && !dto.email().isBlank(),
                dto.telegramId() != null);
        UserProfileRequestAndResponse response = userProfileService.upsertProfile(userId, dto);
        log.info("Api: profile upsert success userId={}", userId);
        return response;
    }

}
