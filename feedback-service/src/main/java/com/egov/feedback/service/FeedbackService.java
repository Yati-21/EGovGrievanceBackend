package com.egov.feedback.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.egov.feedback.dto.FeedbackRequest;
import com.egov.feedback.dto.GrievanceResponse;
import com.egov.feedback.model.Feedback;
import com.egov.feedback.repository.FeedbackRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final WebClient.Builder webClientBuilder;

    public Mono<Feedback> submitFeedback(String citizenId, String role, FeedbackRequest request) {

        if (!"CITIZEN".equalsIgnoreCase(role)) {
            return Mono.error(new IllegalArgumentException("Only CITIZEN can submit feedback"));
        }

        return feedbackRepository.findByGrievanceId(request.getGrievanceId())
                .flatMap(existing -> Mono
                        .<Feedback>error(new IllegalArgumentException("Feedback already submitted for this grievance")))
                .switchIfEmpty(
                        fetchGrievance(request.getGrievanceId())
                                .flatMap(grievance -> {
                                    if (!"CLOSED".equalsIgnoreCase(grievance.getStatus())) {
                                        return Mono.error(new IllegalArgumentException(
                                                "Feedback can only be submitted for CLOSED grievances"));
                                    }

                                    if (!citizenId.equals(grievance.getCitizenId())) {
                                        return Mono.error(new IllegalArgumentException(
                                                "You can only submit feedback for your own grievances"));
                                    }

                                    Feedback feedback = Feedback.builder()
                                            .grievanceId(request.getGrievanceId())
                                            .citizenId(citizenId)
                                            .rating(request.getRating())
                                            .comment(request.getComment())
                                            .createdAt(java.time.Instant.now())
                                            .build();

                                    return feedbackRepository.save(feedback);
                                }));
    }

    private Mono<GrievanceResponse> fetchGrievance(String grievanceId) {
        return webClientBuilder.build()
                .get()
                .uri("http://grievance-service/grievances/{id}", grievanceId)
                .retrieve()
                .onStatus(org.springframework.http.HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Grievance not found")))
                .bodyToMono(GrievanceResponse.class);
    }

    public Mono<Feedback> getFeedbackByGrievanceId(String grievanceId) {
        return feedbackRepository.findByGrievanceId(grievanceId);
    }

    public Mono<Feedback> getFeedbackById(String id) {
        return feedbackRepository.findById(id);
    }
}
