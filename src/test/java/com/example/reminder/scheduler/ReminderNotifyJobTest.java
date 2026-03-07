package com.example.reminder.scheduler;

import com.example.reminder.domain.reminder.ReminderNotificationStatus;
import com.example.reminder.testutil.TestData;
import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.service.notification.NotificationService;
import com.example.reminder.service.scheduler.ReminderNotifyJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderNotifyJobTest {

    @Mock
    ReminderRepository reminderRepository;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    ReminderNotifyJob reminderNotifyJob;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderNotifyJob, "maxAttempts", 3);
    }

    @Test
    void shouldQueryDueRemindersByCurrentInstantAndSkipWhenEmpty() {
        when(reminderRepository.findDueForNotification(anyCollection(), any(), anyInt())).thenReturn(List.of());
        Instant before = Instant.now();

        reminderNotifyJob.run();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Integer> maxAttemptsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reminderRepository).findDueForNotification(anyCollection(), nowCaptor.capture(), maxAttemptsCaptor.capture());
        assertThat(nowCaptor.getValue()).isBetween(before, after);
        assertThat(maxAttemptsCaptor.getValue()).isEqualTo(3);
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldMarkAsSentAfterSuccessfulNotification() {
        Reminder due = TestData.reminderDue(1L);
        due.setNotifiedAt(null);
        when(reminderRepository.findDueForNotification(anyCollection(), any(), anyInt())).thenReturn(List.of(due));
        when(notificationService.notify(due)).thenReturn(true);

        reminderNotifyJob.run();

        verify(notificationService).notify(due);
        assertThat(due.getStatus()).isEqualTo(ReminderNotificationStatus.SENT);
        assertThat(due.getNotifiedAt()).isNotNull();
        assertThat(due.getAttemptCount()).isEqualTo(1);
        assertThat(due.getLastAttemptAt()).isNotNull();
        assertThat(due.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldScheduleRetryWhenNotificationFails() {
        Reminder bad = TestData.reminderDue(2L);
        bad.setNotifiedAt(null);
        when(reminderRepository.findDueForNotification(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(bad));
        when(notificationService.notify(bad)).thenReturn(false);

        reminderNotifyJob.run();

        verify(notificationService).notify(bad);
        assertThat(bad.getStatus()).isEqualTo(ReminderNotificationStatus.FAILED);
        assertThat(bad.getNotifiedAt()).isNull();
        assertThat(bad.getAttemptCount()).isEqualTo(1);
        assertThat(bad.getLastAttemptAt()).isNotNull();
        assertThat(bad.getNextAttemptAt()).isNotNull();
        assertThat(bad.getNextAttemptAt()).isAfter(bad.getLastAttemptAt());
    }

    @Test
    void shouldStopRetryWhenMaxAttemptsReached() {
        Reminder reminder = TestData.reminderDue(10L);
        reminder.setAttemptCount(2);
        reminder.setNotifiedAt(null);
        when(reminderRepository.findDueForNotification(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(reminder));
        when(notificationService.notify(reminder)).thenReturn(false);

        reminderNotifyJob.run();

        verify(notificationService).notify(reminder);
        assertThat(reminder.getAttemptCount()).isEqualTo(3);
        assertThat(reminder.getStatus()).isEqualTo(ReminderNotificationStatus.FAILED);
        assertThat(reminder.getNextAttemptAt()).isNull();
        assertThat(reminder.getNotifiedAt()).isNull();
    }

    @Test
    void shouldTreatThrownNotificationErrorAsFailureAndContinue() {
        Reminder broken = TestData.reminderDue(11L);
        Reminder ok = TestData.reminderDue(12L);
        when(reminderRepository.findDueForNotification(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(broken, ok));
        doThrow(new RuntimeException("boom")).when(notificationService).notify(broken);
        when(notificationService.notify(ok)).thenReturn(true);

        reminderNotifyJob.run();

        verify(notificationService).notify(broken);
        verify(notificationService).notify(ok);
        assertThat(broken.getStatus()).isEqualTo(ReminderNotificationStatus.FAILED);
        assertThat(broken.getNotifiedAt()).isNull();
        assertThat(ok.getStatus()).isEqualTo(ReminderNotificationStatus.SENT);
        assertThat(ok.getNotifiedAt()).isNotNull();
    }
}
