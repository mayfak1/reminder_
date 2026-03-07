package com.example.reminder.service.notification.email;

public interface EmailSender {
    void send(String to,String subject,String body);
}
