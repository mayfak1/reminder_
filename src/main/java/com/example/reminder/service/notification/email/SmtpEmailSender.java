package com.example.reminder.service.notification.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Override
    public void send(String to, String subject, String body) {
        log.debug("Notify: EMAIL prepare to={} subjectPresent={} bodyLength={}",
                to,
                subject != null && !subject.isBlank(),
                body == null ? 0 : body.length());
        SimpleMailMessage msg = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            msg.setFrom(from);
        }
        msg.setTo(to);
        msg.setSubject(ifNullToString(subject));
        msg.setText(ifNullToString(body));
        mailSender.send(msg);
        log.info("Notify: EMAIL sent to={}", to);
    }

    private String ifNullToString(String s) {
        return s == null ? "" : s;
    }



}
