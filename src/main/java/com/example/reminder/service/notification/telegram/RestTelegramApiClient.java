package com.example.reminder.service.notification.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestTelegramApiClient implements TelegramApiClient {
    private final RestClient restClient;

    public RestTelegramApiClient(
            RestClient.Builder builder,
            @Value("${app.telegram.api-base-url:https://api.telegram.org}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void sendMessage(String token, Long chatId, String text) {
        restClient.post()
                .uri("/bot{token}/sendMessage", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SendMessageRequest(chatId, text))
                .retrieve()
                .toBodilessEntity();
    }

    private record SendMessageRequest(Long chat_id, String text) {
    }
}

