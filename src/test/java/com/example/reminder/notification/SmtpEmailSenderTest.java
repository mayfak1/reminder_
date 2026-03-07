package com.example.reminder.notification;

import com.example.reminder.service.notification.email.SmtpEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    JavaMailSender mailSender;

    SmtpEmailSender smtpEmailSender;

    @BeforeEach
    void setUp() {
        smtpEmailSender = new SmtpEmailSender(mailSender);
    }

    @Test
    void shouldSendEmail_whenFromPresent() {
        ReflectionTestUtils.setField(smtpEmailSender, "from", "noreply@test.com");

        smtpEmailSender.send("user@test.com", "subj", "hello");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@test.com");
        assertThat(message.getTo()).containsExactly("user@test.com");
        assertThat(message.getSubject()).isEqualTo("subj");
        assertThat(message.getText()).isEqualTo("hello");
    }

    @Test
    void shouldNotSetFrom_whenFromBlank() {
        ReflectionTestUtils.setField(smtpEmailSender, "from", "");

        smtpEmailSender.send("user@test.com", "subj", "hello");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isNull();
    }

    @Test
    void shouldConvertNullSubjectAndBody_toEmptyString() {
        ReflectionTestUtils.setField(smtpEmailSender, "from", "noreply@test.com");

        smtpEmailSender.send("user@test.com", null, null);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("");
        assertThat(message.getText()).isEqualTo("");
    }

    @Test
    void shouldThrow_whenMailSenderFails() {
        doThrow(new MailSendException("fail")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> smtpEmailSender.send("user@test.com", "subj", "hello"))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("fail");
    }
}

