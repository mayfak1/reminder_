package com.example.reminder.testutil;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.user.User;
import com.example.reminder.domain.user.UserProfile;

import java.time.Instant;

public final class TestData {
    private TestData() {}

    public static User user() {
        User u = new User();
        u.setCreatedAt(Instant.now());
        return u;
    }

    public static UserProfile profileWithEmailAndTelegram(User user) {
        UserProfile p = new UserProfile();
        p.setUser(user);
        p.setEmail("test@test.com");
        p.setTelegramChatId(123L);
        return p;
    }

    public static Reminder reminderWithEmailAndTelegram() {
        User u = user();
        UserProfile p = profileWithEmailAndTelegram(u);
        u.setUserProfile(p);

        Reminder r = new Reminder();
        r.setUser(u);
        r.setTitle("Pay rent");
        r.setDescription("Monthly");
        r.setRemind(Instant.now().plusSeconds(60));
        return r;
    }

    public static Reminder reminderWithEmailOnly() {
        Reminder r = reminderWithEmailAndTelegram();
        r.getUser().getUserProfile().setTelegramChatId(null);
        return r;
    }

    public static Reminder reminderWithNoContacts() {
        Reminder r = reminderWithEmailAndTelegram();
        r.getUser().getUserProfile().setEmail(null);
        r.getUser().getUserProfile().setTelegramChatId(null);
        return r;
    }

    public static Reminder reminderDue(Long id) {
        Reminder r = reminderWithEmailAndTelegram();
        r.setId(id);
        r.setRemind(Instant.now().minusSeconds(10));
        return r;
    }

    public static Reminder reminder(User user, String title, String description, Instant time) {
        Reminder reminder=new Reminder();
        reminder.setUser(user);
        reminder.setTitle(title);
        reminder.setDescription(description);
        reminder.setRemind(time);
        return reminder;
    }
}
