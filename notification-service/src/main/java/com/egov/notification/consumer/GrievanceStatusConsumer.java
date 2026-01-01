package com.egov.notification.consumer;

import com.egov.notification.dto.UserResponse;
import com.egov.notification.event.GrievanceStatusChangedEvent;
import com.egov.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
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

    // cmd to list topics: docker exec -it kafka kafka-topics --bootstrap-server
    // localhost:9092 --list
    @KafkaListener(topics = "grievance-status-changed", groupId = "notification-service")
    public void consume(GrievanceStatusChangedEvent event) {

        log.info("Received grievance event: {}", event);

    	    //always notify citizen
        notifyCitizen(event.getCitizenId(), event);

    	    //notify assigned officer 
        if (event.getAssignedOfficerId() != null &&
                !"ESCALATED".equalsIgnoreCase(event.getNewStatus())) {

            notifyOfficer(event.getAssignedOfficerId(), event);
        }

    	  //Notify supervisor(of that dept) on escalation
        if ("ESCALATED".equalsIgnoreCase(event.getNewStatus()) &&
                event.getAssignedOfficerId() != null) {
            notifySupervisor(event.getAssignedOfficerId(), event);
        }
    }


    private void notifyCitizen(
            String userId,
            GrievanceStatusChangedEvent event) {

        UserResponse user = fetchUser(userId);
        if (user == null) return;

        emailService.sendCitizenMail(
                user.getEmail(),
                user.getName(),
                event.getGrievanceId(),
                event.getOldStatus(),
                event.getNewStatus());
    }

    private void notifyOfficer(
            String officerId,
            GrievanceStatusChangedEvent event) {

        UserResponse user = fetchUser(officerId);
        if (user == null) return;

        emailService.sendOfficerMail(
                user.getEmail(),
                user.getName(),
                user.getRole(),
                event.getGrievanceId(),
                event.getNewStatus()
        );
    }

    private void notifySupervisor(
            String supervisorId,
            GrievanceStatusChangedEvent event) {

        UserResponse user = fetchUser(supervisorId);
        if (user == null) return;

        emailService.sendSupervisorMail(
                user.getEmail(),
                user.getName(),
                user.getRole(),
                event.getGrievanceId());
    }

    private UserResponse fetchUser(String userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://user-service/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch user {}", userId, e);
            return null;
        }
    }

}
