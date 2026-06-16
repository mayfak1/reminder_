package com.example.reminder.service.notification.telegram;

public interface TelegramSender {
    boolean send(Long chatId,String text);
}
