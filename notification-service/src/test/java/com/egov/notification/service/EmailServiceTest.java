package com.egov.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

class EmailServiceTest {

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private EmailService emailService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
	}

	@Test
	void sendCitizenMail_success() {
		emailService.sendCitizenMail("a@test.com", "A", "G1", "OPEN", "CLOSED");

		verify(mailSender).send(any(MimeMessage.class));
	}

	@Test
	void sendOfficerMail_success() {
		emailService.sendOfficerMail("o@test.com", "Officer", "OFFICER", "G1", "IN_PROGRESS");

		verify(mailSender).send(any(MimeMessage.class));
	}

	@Test
	void sendSupervisorMail_success() {
		emailService.sendSupervisorMail("s@test.com", "Supervisor", "SUPERVISOR", "G1");

		verify(mailSender).send(any(MimeMessage.class));
	}
}
