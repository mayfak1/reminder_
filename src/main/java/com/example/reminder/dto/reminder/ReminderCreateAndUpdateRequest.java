package com.example.reminder.dto.reminder;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ReminderCreateAndUpdateRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
        @Size(max = 4096, message = "description must be at most 4096 characters")
        String description,
        @NotNull(message = "remind is required")
        @FutureOrPresent(message = "remind must be in the present or future")
        Instant remind
) {
}
