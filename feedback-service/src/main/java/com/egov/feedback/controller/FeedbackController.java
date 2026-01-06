package com.egov.feedback.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.egov.feedback.dto.FeedbackRequest;
import com.egov.feedback.model.Feedback;
import com.egov.feedback.service.FeedbackService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public Mono<ResponseEntity<String>> submitFeedback(
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader(value = "X-USER-ROLE") String role,
            @Valid @RequestBody FeedbackRequest request) {
        return feedbackService.submitFeedback(userId, role, request)
                .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
    }

    @GetMapping("/grievance/{grievanceId}")
    public Mono<Feedback> getFeedbackByGrievance(
            @PathVariable String grievanceId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return feedbackService.getFeedbackByGrievanceId(grievanceId, userId, role);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Feedback>> getFeedbackById(
            @PathVariable String id,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {

        return feedbackService.getFeedbackById(id, userId, role)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
