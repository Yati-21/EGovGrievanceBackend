package com.egov.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

        private final JavaMailSender mailSender;
        private static final String UTF = "UTF-8";

        public void sendCitizenMail(
                        String to,
                        String name,
                        String grievanceId,
                        String oldStatus,
                        String newStatus) {

                String htmlContent = String.format(
                                """
                                                <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                                                    <h2 style="color: #2c3e50;">Grievance Status Update</h2>
                                                    <p>Dear <b>%s</b>,</p>
                                                    <p>Your grievance (ID: <b>%s</b>) has been updated.</p>
                                                    <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0;">
                                                        <p><b>Previous Status:</b> %s</p>
                                                        <p><b>Current Status:</b> <span style="color: #2980b9;">%s</span></p>
                                                    </div>
                                                    <p>You can track your grievance on the portal.</p>
                                                    <br>
                                                    <p style="font-size: 14px; color: #7f8c8d;">Regards,<br>E-Governance Grievance System</p>
                                                </div>
                                                """,
                                name, grievanceId, oldStatus, newStatus);

                sendHtmlMail(to, "Grievance Status Update", htmlContent);
        }

        public void sendOfficerMail(
                        String to,
                        String name,
                        String designation,
                        String grievanceId,
                        String newStatus) {

                String htmlContent = String.format(
                                """
                                                <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                                                    <h2 style="color: #c0392b;">Action Required</h2>
                                                    <p>Dear <b>%s</b> (%s),</p>
                                                    <p>A grievance has been assigned/updated.</p>
                                                    <div style="background-color: #fff3cd; padding: 15px; border-radius: 5px; border-left: 5px solid #ffc107;">
                                                        <p><b>Grievance ID:</b> %s</p>
                                                        <p><b>Current Status:</b> %s</p>
                                                    </div>
                                                    <p>Please take necessary action.</p>
                                                    <br>
                                                    <p style="font-size: 14px; color: #7f8c8d;">Regards,<br>E-Governance Grievance System</p>
                                                </div>
                                                """,
                                name, designation, grievanceId, newStatus);

                sendHtmlMail(to, "Grievance Action Required", htmlContent);
        }

        public void sendSupervisorMail(
                        String to,
                        String name,
                        String designation,
                        String grievanceId) {

                String htmlContent = String.format(
                                """
                                                <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                                                    <h2 style="color: #e74c3c;">Grievance Escalated</h2>
                                                    <p>Dear <b>%s</b> (%s),</p>
                                                    <p>A grievance has been escalated and requires your attention.</p>
                                                    <div style="background-color: #fadbd8; padding: 15px; border-radius: 5px; border-left: 5px solid #e74c3c;">
                                                        <p><b>Grievance ID:</b> %s</p>
                                                    </div>
                                                    <p>Please review and take appropriate action.</p>
                                                    <br>
                                                    <p style="font-size: 14px; color: #7f8c8d;">Regards,<br>E-Governance Grievance System</p>
                                                </div>
                                                """,
                                name, designation, grievanceId);

                sendHtmlMail(to, "Grievance Escalated", htmlContent);
        }

        private void sendHtmlMail(String to, String subject, String htmlContent) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF);

                        helper.setTo(to);
                        helper.setSubject(subject);
                        helper.setText(htmlContent, true);

                        mailSender.send(message);
                } catch (MessagingException e) {
                        log.error("Failed to send mail to {}", to, e);
                }
        }

}
