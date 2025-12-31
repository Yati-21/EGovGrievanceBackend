package com.egov.feedback.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.egov.feedback.model.Feedback;

import reactor.core.publisher.Mono;

public interface FeedbackRepository
        extends ReactiveMongoRepository<Feedback, String> {

    Mono<Feedback> findByGrievanceId(String grievanceId);
}
