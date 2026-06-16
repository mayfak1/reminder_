package com.example.reminder.domain.reminder;

import com.example.reminder.domain.user.User;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(nullable = false,length = 255)
    private String title;

    @Column(length = 4096)
    private String description;

    @Column(name = "remind", nullable = false)
    private Instant remind;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReminderNotificationStatus status = ReminderNotificationStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;



}
