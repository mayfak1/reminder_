package com.example.reminder.notification;

import com.example.reminder.service.notification.telegram.TelegramBotSender;
import com.example.reminder.service.notification.telegram.TelegramApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TelegramBotSenderTest {

    @Mock
    TelegramApiClient telegramApiClient;

    @Test
    void tokenBlankShouldSkipCall() {
        TelegramBotSender sender = new TelegramBotSender(telegramApiClient, "   ");

        boolean sent = sender.send(123L, "hello");

        verifyNoInteractions(telegramApiClient);
        assertThat(sent).isFalse();
    }

    @Test
    void tokenPresentShouldCallApiClient() {
        TelegramBotSender sender = new TelegramBotSender(telegramApiClient, "TOKEN");

        boolean sent = sender.send(123L, "hello");

        verify(telegramApiClient).sendMessage("TOKEN", 123L, "hello");
        assertThat(sent).isTrue();
    }

    @Test
    void nullTextShouldBeNormalizedToEmptyString() {
        TelegramBotSender sender = new TelegramBotSender(telegramApiClient, "TOKEN");

        boolean sent = sender.send(123L, null);

        verify(telegramApiClient).sendMessage("TOKEN", 123L, "");
        assertThat(sent).isTrue();
    }

    @Test
    void senderShouldPropagateApiErrors() {
        TelegramBotSender sender = new TelegramBotSender(telegramApiClient, "TOKEN");
        doThrow(new RuntimeException("api failed"))
                .when(telegramApiClient).sendMessage("TOKEN", 123L, "hello");

        assertThatThrownBy(() -> sender.send(123L, "hello"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("api failed");
    }
}
