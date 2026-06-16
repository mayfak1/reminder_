package com.example.reminder.scheduler;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.reminder.ReminderNotificationStatus;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.service.notification.NotificationService;
import com.example.reminder.service.scheduler.ReminderNotificationWorker;
import com.example.reminder.testutil.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderNotificationWorkerTest {

    @Mock
    ReminderRepository reminderRepository;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    ReminderNotificationWorker reminderNotificationWorker;

    @Test
    void shouldMarkAsSentAfterSuccessfulNotification() {
        Reminder due = TestData.reminderDue(1L);
        due.setNotifiedAt(null);
        when(reminderRepository.findForNotificationProcessingById(1L)).thenReturn(Optional.of(due));
        when(notificationService.notify(due)).thenReturn(true);

        reminderNotificationWorker.process(1L, 3);

        verify(notificationService).notify(due);
        assertThat(due.getStatus()).isEqualTo(ReminderNotificationStatus.SENT);
        assertThat(due.getNotifiedAt()).isNotNull();
        assertThat(due.getAttemptCount()).isEqualTo(1);
        assertThat(due.getLastAttemptAt()).isNotNull();
        assertThat(due.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldScheduleRetryWhenNotificationFails() {
        Reminder reminder = TestData.reminderDue(2L);
        reminder.setNotifiedAt(null);
        when(reminderRepository.findForNotificationProcessingById(2L)).thenReturn(Optional.of(reminder));
        when(notificationService.notify(reminder)).thenReturn(false);

        reminderNotificationWorker.process(2L, 3);

        verify(notificationService).notify(reminder);
        assertThat(reminder.getStatus()).isEqualTo(ReminderNotificationStatus.FAILED);
        assertThat(reminder.getNotifiedAt()).isNull();
        assertThat(reminder.getAttemptCount()).isEqualTo(1);
        assertThat(reminder.getLastAttemptAt()).isNotNull();
        assertThat(reminder.getNextAttemptAt()).isNotNull();
        assertThat(reminder.getNextAttemptAt()).isAfter(reminder.getLastAttemptAt());
    }

    @Test
    void shouldStopRetryWhenMaxAttemptsReached() {
        Reminder reminder = TestData.reminderDue(10L);
        reminder.setAttemptCount(2);
        reminder.setNotifiedAt(null);
        when(reminderRepository.findForNotificationProcessingById(10L)).thenReturn(Optional.of(reminder));
        when(notificationService.notify(reminder)).thenReturn(false);

        reminderNotificationWorker.process(10L, 3);

        verify(notificationService).notify(reminder);
        assertThat(reminder.getAttemptCount()).isEqualTo(3);
        assertThat(reminder.getStatus()).isEqualTo(ReminderNotificationStatus.FAILED);
        assertThat(reminder.getNextAttemptAt()).isNull();
        assertThat(reminder.getNotifiedAt()).isNull();
    }

    @Test
    void shouldTreatThrownNotificationErrorAsFailure() {
        Reminder reminder = TestData.reminderDue(11L);
        when(reminderRepository.findForNotificationProcessingById(11L)).thenReturn(Optional.of(reminder));
        doThrow(new RuntimeException("boom")).when(notificationService).notify(reminder);

        reminderNotificationWorker.process(11L, 3);

        verify(notificationService).notify(reminder);
        assertThat(reminder.getStatus()).isEqualTo(ReminderNotificationStatus.FAILED);
        assertThat(reminder.getNotifiedAt()).isNull();
        assertThat(reminder.getAttemptCount()).isEqualTo(1);
        assertThat(reminder.getNextAttemptAt()).isNotNull();
    }

    @Test
    void shouldSkipWhenReminderIsNoLongerProcessable() {
        Reminder sent = TestData.reminderDue(12L);
        sent.setStatus(ReminderNotificationStatus.SENT);
        sent.setNotifiedAt(Instant.now());
        when(reminderRepository.findForNotificationProcessingById(12L)).thenReturn(Optional.of(sent));

        reminderNotificationWorker.process(12L, 3);

        verifyNoInteractions(notificationService);
    }
}
