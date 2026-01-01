package com.egov.notification.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

        private final JavaMailSender mailSender;

        public void sendCitizenMail(
                        String to,
                        String name,
                        String grievanceId,
                        String oldStatus,
                        String newStatus) {

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject("Grievance Status Update");
                message.setText(
                                "Dear " + name + ",\n\n" +
                                                "Your grievance (ID: " + grievanceId + ") has been updated.\n\n" +
                                                "Previous Status: " + oldStatus + "\n" +
                                                "Current Status: " + newStatus + "\n\n" +
                                                "You can track your grievance on the portal.\n\n" +
                                                "Regards,\nE-Governance Grievance System");

                mailSender.send(message);
        }

        public void sendOfficerMail(
                        String to,
                        String name,
                        String designation,
                        String grievanceId,
                        String newStatus) {

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject("Grievance Action Required");
                message.setText(
                                "Dear " + name + " (" + designation + "),\n\n" +
                                                "A grievance has been assigned/updated.\n\n" +
                                                "Grievance ID: " + grievanceId + "\n" +
                                                "Current Status: " + newStatus + "\n\n" +
                                                "Please take necessary action.\n\n" +
                                                "Regards,\nE-Governance Grievance System");

                mailSender.send(message);
        }

        public void sendSupervisorMail(
                        String to,
                        String name,
                        String designation,
                        String grievanceId) {

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject("Grievance Escalated");
                message.setText(
                                "Dear " + name + " (" + designation + "),\n\n" +
                                                "A grievance has been escalated and requires your attention.\n\n" +
                                                "Grievance ID: " + grievanceId + "\n\n" +
                                                "Please review and take appropriate action.\n\n" +
                                                "Regards,\nE-Governance Grievance System");

                mailSender.send(message);
        }
}
