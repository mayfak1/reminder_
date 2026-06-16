package com.example.reminder.service.notification;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.user.UserProfile;
import com.example.reminder.service.notification.email.EmailSender;
import com.example.reminder.service.notification.telegram.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final EmailSender emailSender;
    private final TelegramSender telegramSender;
    @Override
    public boolean notify(Reminder reminder) {
        if (reminder == null || reminder.getUser() == null){
            log.warn("Notify: reminder org user is null");
            return false;
        }

        log.debug("Notify: start reminderId={} userId={}", reminder.getId(), reminder.getUser().getId());
        UserProfile profile = reminder.getUser().getUserProfile();
        if (profile == null) {
            log.warn("Notify: userProfile is null, userId={}", reminder.getUser().getId());
            return false;
        }

        String title = safe(reminder.getTitle());
        String description = safe(reminder.getDescription());

        String text = buildText(title, description, reminder.getRemind());
        boolean sent = false;

        String email = profile.getEmail();
        if (email != null && !email.isBlank()) {
            log.info("Notify: sending EMAIL reminderId={} to={}", reminder.getId(), email);
            String subject = "Reminder: " + title;
            try {
                emailSender.send(email, subject, text);
                sent = true;
            } catch (Exception e) {
                log.warn("Notify: EMAIL failed reminderId={} to={} message={}",
                        reminder.getId(), email, e.getMessage(), e);
            }
        }

        Long chatId = profile.getTelegramChatId();
        if (chatId != null) {
            log.info("Notify: sending TELEGRAM reminderId={} chatId={}", reminder.getId(), chatId);
            try {
                boolean telegramSent = telegramSender.send(chatId, text);
                sent = sent || telegramSent;
            } catch (Exception e) {
                log.warn("Notify: TELEGRAM failed reminderId={} chatId={} message={}",
                        reminder.getId(), chatId, e.getMessage(), e);
            }
        }
        if (!sent) {
            log.warn("Notify: no available channels reminderId={} userId={}",
                    reminder.getId(), reminder.getUser().getId());
        }
        return sent;
    }

    private String buildText(String title, String description, Instant remindAt) {
        StringBuilder text = new StringBuilder(title);
        if (!description.isBlank()) {
            text.append("\n\n").append(description);
        }
        if (remindAt != null) {
            text.append("\n\nRemind at: ").append(remindAt);
        }
        return text.toString();
    }

    private String safe(String s) {
        return s==null?"":s.trim();
    }
}
