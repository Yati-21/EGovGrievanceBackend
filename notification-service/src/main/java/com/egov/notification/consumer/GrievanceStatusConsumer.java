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

    //cmd to list topics: docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
    @KafkaListener(
    	    topics = "grievance-status-changed",
    	    groupId = "notification-service"
    	)
	public void consume(GrievanceStatusChangedEvent event) {

	    log.info("Received grievance event: {}", event);

	    //always notify citizen
	    notifyUserById(
	        event.getCitizenId(),
	        event.getGrievanceId(),
	        event.getOldStatus(),
	        event.getNewStatus()
	    );

	    //notify assigned officer (if exists)
	    if (event.getAssignedOfficerId() != null &&
	        !event.getNewStatus().equalsIgnoreCase("ESCALATED")) {

	        notifyUserById(
	            event.getAssignedOfficerId(),
	            event.getGrievanceId(),
	            event.getOldStatus(),
	            event.getNewStatus()
	        );
	    }

	    //Notify supervisor(of that dept) on escalation
	    if ("ESCALATED".equalsIgnoreCase(event.getNewStatus())
	        && event.getAssignedOfficerId() != null) {

	        notifyUserById(
	            event.getAssignedOfficerId(), // supervisorId
	            event.getGrievanceId(),
	            event.getOldStatus(),
	            event.getNewStatus()
	        );
	    }
	}
    
    private void notifyUserById(
            String userId,
            String grievanceId,
            String oldStatus,
            String newStatus) {

        UserResponse user = webClientBuilder.build()
                .get()
                .uri("http://user-service/users/{id}", userId)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();

        if (user == null || user.getEmail() == null) {
            log.error("Email not found for user {}", userId);
            return;
        }

        emailService.sendStatusChangeMail(
                user.getEmail(),
                grievanceId,
                oldStatus,
                newStatus
        );

        log.info("Email sent to {} for grievance {}", user.getEmail(), grievanceId);
    }

}
