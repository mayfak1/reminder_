package com.example.reminder.service.user;

import com.example.reminder.domain.user.User;
import com.example.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {
    private final UserRepository userRepository;
    public Long getOrCreateUserId(Jwt jwt){
        String subject = jwt.getSubject();
        log.debug("Auth: resolving user by subject={}", subject);
        User user = userRepository.findByOauth2Subject(subject)
                .orElseGet(() -> {
                    log.info("Auth: user not found, creating user subject={}", subject);
                    User u = new User();
                    u.setOauth2Subject(subject);
                    return userRepository.save(u);
                });
        log.debug("Auth: resolved userId={} for subject={}", user.getId(), subject);
        return user.getId();

    }
}
