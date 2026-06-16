package com.example.reminder.repository;

import com.example.reminder.testutil.TestData;
import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.reminder.ReminderSpecifications;
import com.example.reminder.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ReminderRepositorySpecTest {

    @Autowired ReminderRepository reminderRepository;
    @Autowired UserRepository userRepository;

    @Test
    void shouldFilterByUserAndQueryAndDate() {
        User u1 = userRepository.save(userWithSubject("sub-u1"));
        User u2 = userRepository.save(userWithSubject("sub-u2"));

        LocalDate date = LocalDate.of(2026, 3, 16);

        reminderRepository.save(TestData.reminder(u1, "Pay rent", "monthly",
                Instant.parse("2026-03-16T10:00:00Z")));

        reminderRepository.save(TestData.reminder(u1, "Buy milk", "shop",
                Instant.parse("2026-03-16T12:00:00Z")));

        reminderRepository.save(TestData.reminder(u1, "Pay rent", "old",
                Instant.parse("2026-02-06T10:00:00Z")));

        reminderRepository.save(TestData.reminder(u2, "Pay rent", "u2",
                Instant.parse("2026-03-16T09:00:00Z")));

        Specification<Reminder> spec = Specification
                .where(ReminderSpecifications.belongsToUser(u1.getId()))
                .and(ReminderSpecifications.search("pay"))
                .and(ReminderSpecifications.filterByDate(date));

        Page<Reminder> page = reminderRepository.findAll(
                spec,
                PageRequest.of(0, 20, Sort.by("remind").ascending())
        );

        assertThat(page.getTotalElements()).isEqualTo(1);

        Reminder found = page.getContent().get(0);
        assertThat(found.getUser().getId()).isEqualTo(u1.getId());
        assertThat(found.getTitle()).containsIgnoringCase("pay");

        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        assertThat(found.getRemind()).isBetween(from, to);
    }

    private User userWithSubject(String subject) {
        User u = new User();
        u.setOauth2Subject(subject);
        u.setCreatedAt(Instant.now());
        return u;
    }
}
