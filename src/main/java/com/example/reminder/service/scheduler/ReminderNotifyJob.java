package com.example.reminder.service.scheduler;

import com.example.reminder.domain.reminder.ReminderNotificationStatus;
import com.example.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private final ReminderRepository reminderRepository;
    private final ReminderNotificationWorker reminderNotificationWorker;

    @Value("${app.notify.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.notify.fixed-delay-ms:30000}")
    public void run(){
        Instant now=Instant.now();
        int effectiveMaxAttempts = Math.max(maxAttempts, 1);
        List<Long> dueIds = reminderRepository.findDueIdsForNotification(
                PROCESSABLE_STATUSES,
                now,
                effectiveMaxAttempts
        );

        if(dueIds.isEmpty()){
            log.debug("ReminderNotifyJob: now={} no due reminders", now);
            return;
        }

        log.info("ReminderNotifyJob: found {} due reminders now={}", dueIds.size(), now);

        for(Long reminderId:dueIds){
            try {
                reminderNotificationWorker.process(reminderId, effectiveMaxAttempts);
            }catch (Exception e){
                log.warn("ReminderNotifyJob: failed to submit reminderId={} message={}",
                        reminderId, e.getMessage(), e);
            }
        }
    }
}
