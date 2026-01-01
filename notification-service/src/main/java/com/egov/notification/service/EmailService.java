package com.egov.notification.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendStatusChangeMail(
            String to,
            String grievanceId,
            String oldStatus,
            String newStatus) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Grievance Status Update");
        message.setText(
                "Grievance ID: " + grievanceId + "\n" +
                "Status changed from " + oldStatus + " to " + newStatus
        );

        mailSender.send(message);
    }
}
