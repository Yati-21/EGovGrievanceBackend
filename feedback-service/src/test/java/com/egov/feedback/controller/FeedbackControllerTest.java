package com.egov.feedback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.egov.feedback.dto.FeedbackRequest;
import com.egov.feedback.model.Feedback;
import com.egov.feedback.service.FeedbackService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

@WebFluxTest(controllers = FeedbackController.class, properties = { "spring.cloud.config.enabled=false",
		"spring.config.import=optional:configserver:" })
class FeedbackControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private FeedbackService feedbackService;

	@Test
	void submitFeedback_success() {
		FeedbackRequest request = FeedbackRequest.builder().grievanceId("G1").rating(5).comment("Good service").build();
		when(feedbackService.submitFeedback(any(), any(), any())).thenReturn(Mono.just("F1"));
		webTestClient.post().uri("/feedback").header("X-USER-ID", "U1").header("X-USER-ROLE", "CITIZEN")
				.contentType(MediaType.APPLICATION_JSON).bodyValue(request).exchange().expectStatus().isCreated()
				.expectBody(String.class).isEqualTo("F1");
	}

	@Test
	void getFeedbackByGrievance_success() {
		Feedback feedback = Feedback.builder().id("F1").grievanceId("G1").build();
		when(feedbackService.getFeedbackByGrievanceId("G1", "U1", "CITIZEN")).thenReturn(Mono.just(feedback));
		webTestClient.get().uri("/feedback/grievance/G1").header("X-USER-ID", "U1").header("X-USER-ROLE", "CITIZEN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void getFeedbackById_found() {
		Feedback feedback = Feedback.builder().id("F1").build();
		when(feedbackService.getFeedbackById("F1", "U1", "ADMIN")).thenReturn(Mono.just(feedback));
		webTestClient.get().uri("/feedback/F1").header("X-USER-ID", "U1").header("X-USER-ROLE", "ADMIN").exchange()
				.expectStatus().isOk();
	}

	@Test
	void getFeedbackById_notFound() {
		when(feedbackService.getFeedbackById("F1", "U1", "ADMIN")).thenReturn(Mono.empty());
		webTestClient.get().uri("/feedback/F1").header("X-USER-ID", "U1").header("X-USER-ROLE", "ADMIN").exchange()
				.expectStatus().isNotFound();
	}
}
