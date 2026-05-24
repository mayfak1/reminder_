package com.example.reminder.repository;

import com.example.reminder.testutil.TestData;
import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.reminder.ReminderNotificationStatus;
import com.example.reminder.domain.reminder.ReminderSpecifications;
import com.example.reminder.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ReminderRepositorySpecAdditionalTest {

    @Autowired
    ReminderRepository reminderRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void shouldIgnoreBlankQueryAndNullDateFilter() {
        User u1 = userRepository.save(userWithSubject("spec-u1"));
        User u2 = userRepository.save(userWithSubject("spec-u2"));

        reminderRepository.save(TestData.reminder(u1, "Pay rent", "monthly", Instant.parse("2026-03-16T10:00:00Z")));
        reminderRepository.save(TestData.reminder(u1, "Buy milk", "shop", Instant.parse("2026-03-16T11:00:00Z")));
        reminderRepository.save(TestData.reminder(u2, "Pay rent", "u2", Instant.parse("2026-03-16T12:00:00Z")));

        Specification<Reminder> spec = Specification
                .where(ReminderSpecifications.belongsToUser(u1.getId()))
                .and(ReminderSpecifications.search("   "))
                .and(ReminderSpecifications.filterByDate(null));

        Page<Reminder> page = reminderRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(r -> r.getUser().getId().equals(u1.getId()));
    }

    @Test
    void shouldSearchCaseInsensitiveInTitleAndDescription() {
        User user = userRepository.save(userWithSubject("search-u1"));

        reminderRepository.save(TestData.reminder(user, "Pay rent", "monthly", Instant.parse("2026-03-16T10:00:00Z")));
        reminderRepository.save(TestData.reminder(user, "Bills", "PAY internet", Instant.parse("2026-03-16T11:00:00Z")));
        reminderRepository.save(TestData.reminder(user, "Buy milk", "shop", Instant.parse("2026-03-16T12:00:00Z")));

        Specification<Reminder> spec = Specification
                .where(ReminderSpecifications.belongsToUser(user.getId()))
                .and(ReminderSpecifications.search("pay"));

        Page<Reminder> page = reminderRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldFilterDateWithInclusiveStartAndExclusiveEnd() {
        User user = userRepository.save(userWithSubject("date-u1"));
        LocalDate date = LocalDate.of(2026, 3, 16);
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        reminderRepository.save(TestData.reminder(user, "At start", "included", from));
        reminderRepository.save(TestData.reminder(user, "Before end", "included", to.minusSeconds(1)));
        reminderRepository.save(TestData.reminder(user, "At end", "excluded", to));

        Specification<Reminder> spec = Specification
                .where(ReminderSpecifications.belongsToUser(user.getId()))
                .and(ReminderSpecifications.filterByDate(date));

        Page<Reminder> page = reminderRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(r ->
                !r.getRemind().isBefore(from) && r.getRemind().isBefore(to)
        );
    }

    @Test
    void shouldFindOnlyDueIdsForNotification() {
        User user = userRepository.save(userWithSubject("notify-u1"));
        Instant now = Instant.parse("2026-03-16T12:00:00Z");

        Reminder due = reminderRepository.save(TestData.reminder(user, "Due", "pending", now.minusSeconds(60)));

        Reminder retryDue = TestData.reminder(user, "Retry", "failed", now.minusSeconds(30));
        retryDue.setStatus(ReminderNotificationStatus.FAILED);
        retryDue.setAttemptCount(1);
        retryDue.setNextAttemptAt(now.minusSeconds(1));
        retryDue = reminderRepository.save(retryDue);

        Reminder future = TestData.reminder(user, "Future", "pending", now.plusSeconds(60));
        reminderRepository.save(future);

        Reminder retryLater = TestData.reminder(user, "Retry later", "failed", now.minusSeconds(30));
        retryLater.setStatus(ReminderNotificationStatus.FAILED);
        retryLater.setAttemptCount(1);
        retryLater.setNextAttemptAt(now.plusSeconds(60));
        reminderRepository.save(retryLater);

        Reminder exhausted = TestData.reminder(user, "Exhausted", "failed", now.minusSeconds(30));
        exhausted.setStatus(ReminderNotificationStatus.FAILED);
        exhausted.setAttemptCount(3);
        reminderRepository.save(exhausted);

        Reminder sent = TestData.reminder(user, "Sent", "already sent", now.minusSeconds(30));
        sent.setStatus(ReminderNotificationStatus.SENT);
        reminderRepository.save(sent);

        List<Long> dueIds = reminderRepository.findDueIdsForNotification(
                List.of(ReminderNotificationStatus.PENDING, ReminderNotificationStatus.FAILED),
                now,
                3
        );

        assertThat(dueIds).containsExactly(due.getId(), retryDue.getId());
    }

    private User userWithSubject(String subject) {
        User u = new User();
        u.setOauth2Subject(subject);
        u.setCreatedAt(Instant.now());
        return u;
    }
}
