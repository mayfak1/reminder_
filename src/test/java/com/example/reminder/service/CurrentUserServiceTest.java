package com.example.reminder.service;

import com.example.reminder.domain.user.User;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CurrentUserService currentUserService;

    @Mock
    Jwt jwt;

    @Test
    void shouldReturnExistingUserId_whenUserExists() {
        User user = new User();
        user.setId(42L);
        user.setOauth2Subject("subject-1");
        when(jwt.getSubject()).thenReturn("subject-1");
        when(userRepository.findByOauth2Subject("subject-1")).thenReturn(Optional.of(user));

        Long result = currentUserService.getOrCreateUserId(jwt);

        assertThat(result).isEqualTo(42L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldCreateUser_whenUserDoesNotExist() {
        when(jwt.getSubject()).thenReturn("subject-new");
        when(userRepository.findByOauth2Subject("subject-new")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Long result = currentUserService.getOrCreateUserId(jwt);

        assertThat(result).isEqualTo(100L);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getOauth2Subject()).isEqualTo("subject-new");
    }
}

