package com.example.reminder.service.notification;

import com.example.reminder.domain.reminder.Reminder;

public interface NotificationService {
    boolean notify(Reminder reminder);
}
