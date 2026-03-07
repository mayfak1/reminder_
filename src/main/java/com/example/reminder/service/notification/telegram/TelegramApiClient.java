package com.example.reminder.service.notification.telegram;

public interface TelegramApiClient {
    void sendMessage(String token, Long chatId, String text);
}

