package com.example.reminder.dto.reminder;

import java.time.Instant;

public record ReminderResponse(
        Long id,
        String title,
        String description,
        Instant remind
) {
}
