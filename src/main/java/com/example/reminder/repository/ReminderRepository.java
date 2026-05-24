package com.example.reminder.repository;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.reminder.ReminderNotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> ,
        JpaSpecificationExecutor<Reminder> {
    Optional<Reminder> findByIdAndUserId(Long reminderId, Long userId);

    Collection<Reminder> findAllByUserId(Long userId);

    @Query("""
            select r.id
            from Reminder r
            where r.status in :statuses
              and r.remind <= :now
              and r.attemptCount < :maxAttempts
              and (r.nextAttemptAt is null or r.nextAttemptAt <= :now)
            order by r.remind asc
            """)
    List<Long> findDueIdsForNotification(
            @Param("statuses") Collection<ReminderNotificationStatus> statuses,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user","user.userProfile"})
    @Query("""
            select r
            from Reminder r
            where r.id = :id
            """)
    Optional<Reminder> findForNotificationProcessingById(@Param("id") Long id);
}
