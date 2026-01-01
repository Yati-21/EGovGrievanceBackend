package com.egov.notification.consumer;

import com.egov.notification.dto.UserResponse;
import com.egov.notification.event.GrievanceStatusChangedEvent;
import com.egov.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrievanceStatusConsumer {

    private final EmailService emailService;
    private final WebClient.Builder webClientBuilder;

    //cmd to list topics: docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
    @KafkaListener(
        topics = "grievance-status-changed",
        groupId = "notification-service"
    )
    public void consume(GrievanceStatusChangedEvent event) {

        log.info("Received grievance event: {}", event);

        UserResponse user = webClientBuilder.build()
                .get()
                .uri("http://user-service/users/{id}", event.getCitizenId())
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();

        if (user == null || user.getEmail() == null) {
            log.error("Fetched email is NULL for user {}", event.getCitizenId());
            return;
        }

        String email = user.getEmail();
        log.info("Fetched email: '{}'", email);

        emailService.sendStatusChangeMail(
                email,
                event.getGrievanceId(),
                event.getOldStatus(),
                event.getNewStatus());

        log.info("Email sent to {} for grievance {}",
                email,
                event.getGrievanceId());
    }
}
