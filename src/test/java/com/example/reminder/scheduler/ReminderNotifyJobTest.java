package com.example.reminder.scheduler;

import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.service.scheduler.ReminderNotificationWorker;
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
    ReminderNotificationWorker reminderNotificationWorker;

    @InjectMocks
    ReminderNotifyJob reminderNotifyJob;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderNotifyJob, "maxAttempts", 3);
    }

    @Test
    void shouldQueryDueRemindersByCurrentInstantAndSkipWhenEmpty() {
        when(reminderRepository.findDueIdsForNotification(anyCollection(), any(), anyInt())).thenReturn(List.of());
        Instant before = Instant.now();

        reminderNotifyJob.run();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Integer> maxAttemptsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reminderRepository).findDueIdsForNotification(anyCollection(), nowCaptor.capture(), maxAttemptsCaptor.capture());
        assertThat(nowCaptor.getValue()).isBetween(before, after);
        assertThat(maxAttemptsCaptor.getValue()).isEqualTo(3);
        verifyNoInteractions(reminderNotificationWorker);
    }

    @Test
    void shouldSubmitEachDueReminderToWorker() {
        when(reminderRepository.findDueIdsForNotification(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(1L, 2L));

        reminderNotifyJob.run();

        verify(reminderNotificationWorker).process(1L, 3);
        verify(reminderNotificationWorker).process(2L, 3);
    }

    @Test
    void shouldUseAtLeastOneMaxAttempt() {
        ReflectionTestUtils.setField(reminderNotifyJob, "maxAttempts", 0);
        when(reminderRepository.findDueIdsForNotification(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(10L));

        reminderNotifyJob.run();

        ArgumentCaptor<Integer> maxAttemptsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reminderRepository).findDueIdsForNotification(anyCollection(), any(), maxAttemptsCaptor.capture());
        assertThat(maxAttemptsCaptor.getValue()).isEqualTo(1);
        verify(reminderNotificationWorker).process(10L, 1);
    }

    @Test
    void shouldContinueSubmittingWhenOneWorkerCallFails() {
        when(reminderRepository.findDueIdsForNotification(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("queue is full"))
                .when(reminderNotificationWorker).process(1L, 3);

        reminderNotifyJob.run();

        verify(reminderNotificationWorker).process(1L, 3);
        verify(reminderNotificationWorker).process(2L, 3);
    }
}
