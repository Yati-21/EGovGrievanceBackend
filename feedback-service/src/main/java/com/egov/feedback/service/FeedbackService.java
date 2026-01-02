package com.egov.feedback.service;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.egov.feedback.dto.FeedbackRequest;
import com.egov.feedback.dto.GrievanceResponse;
import com.egov.feedback.exception.AccessDeniedException;
import com.egov.feedback.model.Feedback;
import com.egov.feedback.repository.FeedbackRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final WebClient.Builder webClientBuilder;

    public Mono<String> submitFeedback(String citizenId, String role, FeedbackRequest request) {

        if (!"CITIZEN".equalsIgnoreCase(role)) {
            return Mono.error(new AccessDeniedException("Only CITIZEN can submit feedback"));
        }

        return feedbackRepository.findByGrievanceId(request.getGrievanceId())
                .flatMap(existing -> Mono
                        .<String>error(new IllegalArgumentException("Feedback already submitted for this grievance")))
                .switchIfEmpty(
                        fetchGrievance(request.getGrievanceId(), citizenId, role)
                                .flatMap(grievance -> {
                                    if (!"CLOSED".equalsIgnoreCase(grievance.getStatus())) {
                                        return Mono.error(new IllegalArgumentException(
                                                "Feedback can only be submitted for CLOSED grievances"));
                                    }

                                    if (!citizenId.equals(grievance.getCitizenId())) {
                                        return Mono.error(new AccessDeniedException("You can only submit feedback for your own grievances"));
                                    }

                                    Feedback feedback = Feedback.builder()
                                            .grievanceId(request.getGrievanceId())
                                            .citizenId(citizenId)
                                            .rating(request.getRating())
                                            .comment(request.getComment())
                                            .createdAt(java.time.Instant.now())
                                            .build();

                                    return feedbackRepository.save(feedback).map(Feedback::getId);
                                }));
    }

    // for verify access by calling grievance-service
    private Mono<GrievanceResponse> fetchGrievance(String grievanceId, String userId, String role) {
        return webClientBuilder.build()
                .get()
                .uri("http://grievance-service/grievances/{id}", grievanceId)
                .header("X-USER-ID", userId)
                .header("X-USER-ROLE", role)
                .retrieve()
                .onStatus(org.springframework.http.HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Grievance not found")))
                .bodyToMono(GrievanceResponse.class);
    }

    public Mono<Feedback> getFeedbackByGrievanceId(String grievanceId, String userId, String role) {
        return fetchGrievance(grievanceId, userId, role)
                .flatMap(grievance -> feedbackRepository.findByGrievanceId(grievanceId));
    }

    public Mono<Feedback> getFeedbackById(String id, String userId, String role) {
        return feedbackRepository.findById(id)
                .flatMap(feedback -> fetchGrievance(feedback.getGrievanceId(), userId, role)
                        .thenReturn(feedback));
    }
}
