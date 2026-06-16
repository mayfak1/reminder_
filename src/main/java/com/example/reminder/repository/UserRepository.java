package com.example.reminder.repository;

import com.example.reminder.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByOauth2Subject(String oauth2Subject);
}
