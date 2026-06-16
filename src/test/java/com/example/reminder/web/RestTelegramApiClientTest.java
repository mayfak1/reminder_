package com.example.reminder.web;

import com.example.reminder.service.notification.telegram.RestTelegramApiClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestTelegramApiClientTest {

    private MockWebServer mockWebServer;
    private RestTelegramApiClient telegramApiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        telegramApiClient = new RestTelegramApiClient(
                RestClient.builder(),
                mockWebServer.url("/").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldSendPostRequest_whenResponseIs200() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        telegramApiClient.sendMessage("T", 123L, "hi");

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/botT/sendMessage");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        String body = request.getBody().readUtf8();
        assertThat(body).contains("123");
        assertThat(body).contains("\"text\":\"hi\"");
    }

    @Test
    void shouldThrowException_whenResponseIs500() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("error"));

        assertThatThrownBy(() -> telegramApiClient.sendMessage("T", 123L, "hi"))
                .isInstanceOf(RestClientResponseException.class);
    }
}

