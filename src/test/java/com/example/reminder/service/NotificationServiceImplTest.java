package com.example.reminder.service;

import com.example.reminder.testutil.TestData;
import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.service.notification.email.EmailSender;
import com.example.reminder.service.notification.NotificationServiceImpl;
import com.example.reminder.service.notification.telegram.TelegramSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    EmailSender emailSender;

    @Mock
    TelegramSender telegramSender;

    @InjectMocks
    NotificationServiceImpl notificationService;

    @Test
    void shouldSendEmailAndTelegram_whenBothPresent() {
        Reminder reminder = TestData.reminderWithEmailAndTelegram();
        when(telegramSender.send(eq(123L), anyString())).thenReturn(true);

        boolean sent = notificationService.notify(reminder);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailTextCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> telegramTextCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailSender).send(eq("test@test.com"), subjectCaptor.capture(), emailTextCaptor.capture());
        verify(telegramSender).send(eq(123L), telegramTextCaptor.capture());
        verifyNoMoreInteractions(emailSender, telegramSender);

        assertThat(subjectCaptor.getValue()).contains("Pay rent");
        assertThat(emailTextCaptor.getValue()).contains("Pay rent").contains("Remind at:");
        assertThat(telegramTextCaptor.getValue()).contains("Pay rent").contains("Remind at:");
        assertThat(sent).isTrue();
    }

    @Test
    void shouldSendOnlyEmail_whenTelegramMissing() {
        Reminder reminder = TestData.reminderWithEmailOnly();

        boolean sent = notificationService.notify(reminder);

        verify(emailSender).send(eq("test@test.com"), anyString(), anyString());
        verifyNoInteractions(telegramSender);
        assertThat(sent).isTrue();
    }

    @Test
    void shouldSendOnlyTelegram_whenEmailMissing() {
        Reminder reminder = TestData.reminderWithEmailAndTelegram();
        reminder.getUser().getUserProfile().setEmail(null);
        when(telegramSender.send(eq(123L), anyString())).thenReturn(true);

        boolean sent = notificationService.notify(reminder);

        verify(telegramSender).send(eq(123L), anyString());
        verifyNoInteractions(emailSender);
        assertThat(sent).isTrue();
    }

    @Test
    void shouldSendNothing_whenNoContacts() {
        Reminder reminder = TestData.reminderWithNoContacts();

        boolean sent = notificationService.notify(reminder);

        verifyNoInteractions(emailSender, telegramSender);
        assertThat(sent).isFalse();
    }

    @Test
    void shouldSkipWhenReminderOrUserIsNull() {
        boolean first = notificationService.notify(null);

        Reminder reminderWithoutUser = new Reminder();
        boolean second = notificationService.notify(reminderWithoutUser);

        verifyNoInteractions(emailSender, telegramSender);
        assertThat(first).isFalse();
        assertThat(second).isFalse();
    }

    @Test
    void shouldReturnTrueWhenEmailFailsButTelegramSucceeds() {
        Reminder reminder = TestData.reminderWithEmailAndTelegram();
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).send(eq("test@test.com"), anyString(), anyString());
        when(telegramSender.send(eq(123L), anyString())).thenReturn(true);

        boolean sent = notificationService.notify(reminder);

        verify(emailSender).send(eq("test@test.com"), anyString(), anyString());
        verify(telegramSender).send(eq(123L), anyString());
        assertThat(sent).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEveryChannelFails() {
        Reminder reminder = TestData.reminderWithEmailAndTelegram();
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).send(eq("test@test.com"), anyString(), anyString());
        when(telegramSender.send(eq(123L), anyString()))
                .thenThrow(new RuntimeException("telegram down"));

        boolean sent = notificationService.notify(reminder);

        verify(emailSender).send(eq("test@test.com"), anyString(), anyString());
        verify(telegramSender).send(eq(123L), anyString());
        assertThat(sent).isFalse();
    }
}
