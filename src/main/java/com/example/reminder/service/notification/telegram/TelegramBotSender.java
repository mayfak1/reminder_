package com.example.reminder.service.notification.telegram;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TelegramBotSender implements TelegramSender{
    private final TelegramApiClient telegramApiClient;
    private final String token;

    public TelegramBotSender(
            TelegramApiClient telegramApiClient,
            @Value("${app.telegram.bot-token:}") String token
    ){
        this.telegramApiClient = telegramApiClient;
        this.token = token;
    }

    @Override
    public boolean send(Long chatId,String text){
        if (token == null || token.isBlank()) {
            log.warn("Notify: TELEGRAM skipped because bot token is empty chatId={}", chatId);
            return false;
        }
        String normalizedText = text == null ? "" : text;
        log.debug("Notify: TELEGRAM sending chatId={} textLength={}", chatId, normalizedText.length());
        try {
            telegramApiClient.sendMessage(token, chatId, normalizedText);
            log.info("Notify: TELEGRAM sent chatId={}", chatId);
            return true;
        } catch (Exception e) {
            log.warn("Notify: TELEGRAM failed chatId={} message={}", chatId, e.getMessage(), e);
            throw e;
        }

    }
}
