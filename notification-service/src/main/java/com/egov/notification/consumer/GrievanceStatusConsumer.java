package com.egov.notification.consumer;

import com.egov.notification.event.GrievanceStatusChangedEvent;
import com.egov.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrievanceStatusConsumer {

    private final EmailService emailService;

    @KafkaListener(
        topics = "grievance-status-changed",
        groupId = "notification-service"
    )
    public void consume(GrievanceStatusChangedEvent event) {

        log.info("Received grievance event: {}", event);

        // For now hardcode email (later fetch from user-service)
        String email = "citizen@test.com";

        emailService.sendStatusChangeMail(
                email,
                event.getGrievanceId(),
                event.getOldStatus(),
                event.getNewStatus()
        );

        log.info("Email sent for grievance {}", event.getGrievanceId());
    }
}
