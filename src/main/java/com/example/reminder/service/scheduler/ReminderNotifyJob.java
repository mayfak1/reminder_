package com.example.reminder.service.scheduler;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.reminder.ReminderNotificationStatus;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderNotifyJob {
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
    @Value("${app.notify.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.notify.fixed-delay-ms:30000}")
    @Transactional
    public void run(){
        Instant now=Instant.now();
        int effectiveMaxAttempts = Math.max(maxAttempts, 1);
        List<Reminder> due = reminderRepository.findDueForNotification(
                PROCESSABLE_STATUSES,
                now,
                effectiveMaxAttempts
        );

        if(due.isEmpty()){
            log.debug("ReminderNotifyJob: now={} no due reminders", now);
            return;
        }

        log.info("ReminderNotifyJob: found {} due reminders now={}", due.size(), now);

        for(Reminder reminder:due){
            try {
                processReminder(reminder, now, effectiveMaxAttempts);
            }catch (Exception e){
                log.warn("ReminderNotifyJob: unexpected failure reminderId={} message={}",
                        reminder.getId(), e.getMessage(), e);
            }
        }
    }

    private void processReminder(Reminder reminder, Instant now, int effectiveMaxAttempts) {
        int nextAttempt = safeAttempts(reminder) + 1;
        reminder.setAttemptCount(nextAttempt);
        reminder.setLastAttemptAt(now);

        boolean sent;
        try {
            sent = notificationService.notify(reminder);
        } catch (Exception e) {
            log.warn("ReminderNotifyJob: notify threw exception reminderId={} message={}",
                    reminder.getId(), e.getMessage(), e);
            sent = false;
        }

        if (sent) {
            reminder.setStatus(ReminderNotificationStatus.SENT);
            reminder.setNotifiedAt(now);
            reminder.setNextAttemptAt(null);
            log.info("ReminderNotifyJob: sent reminderId={} attempt={}", reminder.getId(), nextAttempt);
            return;
        }

        reminder.setStatus(ReminderNotificationStatus.FAILED);
        if (nextAttempt >= effectiveMaxAttempts) {
            reminder.setNextAttemptAt(null);
            log.warn("ReminderNotifyJob: exhausted retries reminderId={} attempts={}",
                    reminder.getId(), nextAttempt);
            return;
        }

        Duration backoff = resolveBackoff(nextAttempt);
        reminder.setNextAttemptAt(now.plus(backoff));
        log.info("ReminderNotifyJob: scheduled retry reminderId={} attempt={} nextAttemptAt={} backoffSeconds={}",
                reminder.getId(), nextAttempt, reminder.getNextAttemptAt(), backoff.toSeconds());
    }

    private int safeAttempts(Reminder reminder) {
        return reminder.getAttemptCount() == null ? 0 : reminder.getAttemptCount();
    }

    private Duration resolveBackoff(int attemptNumber) {
        int index = Math.max(0, Math.min(attemptNumber - 1, BACKOFF_STEPS.size() - 1));
        return BACKOFF_STEPS.get(index);
    }
}
