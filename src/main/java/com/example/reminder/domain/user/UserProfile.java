package com.example.reminder.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.mapstruct.control.MappingControl;
//ctr+cmd+g
@Entity
@Table(name ="user_profile")
@Getter
@Setter
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(name = "telegram_chat_id",unique = true)
    private Long telegramChatId;
}
