package com.example.reminder.domain.user;

import com.example.reminder.domain.reminder.Reminder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "users",
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_oauth2_subject",columnNames ="oauth2_subject" )
        }
)
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oauth2_subject",
            nullable = false,
            unique = true)
    private String oauth2Subject;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "user")
    private List<Reminder> reminders;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private UserProfile userProfile;

    @PrePersist
    public void prePersist(){
        if (createdAt == null) {
            createdAt=Instant.now();
        }
    }
}
