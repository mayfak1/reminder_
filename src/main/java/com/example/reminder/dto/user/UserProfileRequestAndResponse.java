package com.example.reminder.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UserProfileRequestAndResponse(
        @NotBlank @Email  String email,
        @Positive(message = "telegramId must be positive")
        Long telegramId
) {
}
