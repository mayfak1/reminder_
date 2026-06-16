package com.example.reminder.service.user;

import com.example.reminder.dto.user.UserProfileRequestAndResponse;

public interface UserProfileService {
    UserProfileRequestAndResponse getProfile(Long userId);
    UserProfileRequestAndResponse upsertProfile(Long userId,UserProfileRequestAndResponse request);
}
