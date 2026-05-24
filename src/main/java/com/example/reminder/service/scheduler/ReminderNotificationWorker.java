package com.example.reminder.service.scheduler;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.reminder.ReminderNotificationStatus;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderNotificationWorker {
    private static final List<ReminderNotificationStatus> PROCESSABLE_STATUSES = List.of(
            ReminderNotificationStatus.PENDING,
            ReminderNotificationStatus.FAILED
    );
    private static final List<Duration> BACKOFF_STEPS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15)
    );

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @Transactional
    public void process(Long reminderId, int maxAttempts){
        try {
            processInternal(reminderId, maxAttempts);
        } catch (Exception e) {
            log.warn("ReminderWorker: unexpected failure reminderId={} message={}",
                    reminderId, e.getMessage(), e);
        }
    }

    private void processInternal(Long reminderId, int maxAttempts) {
        Instant now = Instant.now();
        int effectiveMaxAttempts = Math.max(maxAttempts, 1);

        Optional<Reminder> optionalReminder = reminderRepository.findForNotificationProcessingById(reminderId);
        if (optionalReminder.isEmpty()) {
            log.warn("ReminderWorker: reminder not found reminderId={}", reminderId);
            return;
        }

        Reminder reminder = optionalReminder.get();
        if (!isProcessable(reminder, now, effectiveMaxAttempts)) {
            log.debug("ReminderWorker: skip reminderId={} status={} remindAt={} nextAttemptAt={} attempts={}",
                    reminderId,
                    reminder.getStatus(),
                    reminder.getRemind(),
                    reminder.getNextAttemptAt(),
                    safeAttempts(reminder));
            return;
        }

        int nextAttempt = safeAttempts(reminder) + 1;
        reminder.setAttemptCount(nextAttempt);
        reminder.setLastAttemptAt(now);

        boolean sent;
        try {
            sent = notificationService.notify(reminder);
        } catch (Exception e) {
            log.warn("ReminderWorker: notify threw exception reminderId={} message={}",
                    reminderId, e.getMessage(), e);
            sent = false;
        }

        if (sent) {
            reminder.setStatus(ReminderNotificationStatus.SENT);
            reminder.setNotifiedAt(now);
            reminder.setNextAttemptAt(null);
            log.info("ReminderWorker: sent reminderId={} attempt={}", reminderId, nextAttempt);
            return;
        }

        reminder.setStatus(ReminderNotificationStatus.FAILED);
        if (nextAttempt >= effectiveMaxAttempts) {
            reminder.setNextAttemptAt(null);
            log.warn("ReminderWorker: exhausted retries reminderId={} attempts={}",
                    reminderId, nextAttempt);
            return;
        }

        Duration backoff = resolveBackoff(nextAttempt);
        reminder.setNextAttemptAt(now.plus(backoff));
        log.info("ReminderWorker: scheduled retry reminderId={} attempt={} nextAttemptAt={} backoffSeconds={}",
                reminderId, nextAttempt, reminder.getNextAttemptAt(), backoff.toSeconds());
    }

    private boolean isProcessable(Reminder reminder, Instant now, int effectiveMaxAttempts) {
        return PROCESSABLE_STATUSES.contains(reminder.getStatus())
                && !reminder.getRemind().isAfter(now)
                && safeAttempts(reminder) < effectiveMaxAttempts
                && (reminder.getNextAttemptAt() == null || !reminder.getNextAttemptAt().isAfter(now));
    }

    private int safeAttempts(Reminder reminder) {
        return reminder.getAttemptCount() == null ? 0 : reminder.getAttemptCount();
    }

    private Duration resolveBackoff(int attemptNumber) {
        int index = Math.max(0, Math.min(attemptNumber - 1, BACKOFF_STEPS.size() - 1));
        return BACKOFF_STEPS.get(index);
    }
}
