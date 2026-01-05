//package com.egov.feedback.service;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//import com.egov.feedback.dto.FeedbackRequest;
//import com.egov.feedback.dto.GrievanceResponse;
//import com.egov.feedback.exception.AccessDeniedException;
//import com.egov.feedback.model.Feedback;
//import com.egov.feedback.repository.FeedbackRepository;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
////@ExtendWith(MockitoExtension.class)
////class FeedbackServiceTest {
////
////    @Mock
////    private FeedbackRepository feedbackRepository;
////
////    @Mock
////    private WebClient.Builder webClientBuilder;
////
////    @InjectMocks
////    private FeedbackService feedbackService;
////
////    @BeforeEach
////    void setupWebClient() {
////
////        WebClient webClient = mock(WebClient.class);
////        WebClient.RequestHeadersUriSpec uriSpec =
////                mock(WebClient.RequestHeadersUriSpec.class);
////        WebClient.RequestHeadersSpec headersSpec =
////                mock(WebClient.RequestHeadersSpec.class);
////        WebClient.ResponseSpec responseSpec =
////                mock(WebClient.ResponseSpec.class);
////
////        when(webClientBuilder.build()).thenReturn(webClient);
////        when(webClient.get()).thenReturn(uriSpec);
////
////        when(uriSpec.uri(anyString(), any(Object[].class)))
////                .thenReturn(headersSpec);
////
////        // 🔑 REQUIRED: header() must return SAME spec
////        when(headersSpec.header(anyString(), anyString()))
////                .thenReturn(headersSpec);
////
////        when(headersSpec.retrieve())
////                .thenReturn(responseSpec);
////
////        when(responseSpec.onStatus(any(), any()))
////                .thenReturn(responseSpec);
////
////        when(responseSpec.bodyToMono(GrievanceResponse.class))
////                .thenReturn(Mono.just(
////                        new GrievanceResponse("G1", "CLOSED", "U1")
////                ));
////    }
////
////    @Test
////    void submitFeedback_nonCitizen_forbidden() {
////        StepVerifier.create(
////                feedbackService.submitFeedback("U1", "ADMIN",
////                        new FeedbackRequest("G1", 5, "ok"))
////        )
////        .expectError(AccessDeniedException.class)
////        .verify();
////    }
////
////    @Test
////    void submitFeedback_alreadyExists() {
////        when(feedbackRepository.findByGrievanceId("G1"))
////                .thenReturn(Mono.just(new Feedback()));
////
////        StepVerifier.create(
////                feedbackService.submitFeedback("U1", "CITIZEN",
////                        new FeedbackRequest("G1", 5, "ok"))
////        )
////        .expectError(IllegalArgumentException.class)
////        .verify();
////    }
////
////    @Test
////    void getFeedbackById_success() {
////        Feedback feedback = Feedback.builder().id("F1").grievanceId("G1").build();
////
////        when(feedbackRepository.findById("F1"))
////                .thenReturn(Mono.just(feedback));
////
////        // mock fetchGrievance() via WebClient shortcut
////        WebClient webClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
////        when(webClientBuilder.build()).thenReturn(webClient);
////        when(webClient.get().uri(anyString(), any(Object[].class))
////                .retrieve().bodyToMono(GrievanceResponse.class))
////                .thenReturn(Mono.just(new GrievanceResponse("G1", "CLOSED", "U1")));
////
////        StepVerifier.create(
////                feedbackService.getFeedbackById("F1", "U1", "ADMIN")
////        )
////        .expectNext(feedback)
////        .verifyComplete();
////    }
////}
//////@ExtendWith(MockitoExtension.class)
//////class FeedbackServiceTest {
//////
//////    @Mock
//////    private FeedbackRepository feedbackRepository;
//////
//////    @Mock
//////    private WebClient.Builder webClientBuilder;
//////
//////    @InjectMocks
//////    private FeedbackService feedbackService;
//////
//////    @Test
//////    void submitFeedback_nonCitizen_forbidden() {
//////
//////        StepVerifier.create(
//////                feedbackService.submitFeedback(
//////                        "U1", "ADMIN",
//////                        new FeedbackRequest("G1", 5, "ok")
//////                )
//////        )
//////        .expectError(AccessDeniedException.class)
//////        .verify();
//////    }
//////
//////    @Test
//////    void submitFeedback_alreadyExists() {
//////        when(feedbackRepository.findByGrievanceId("G1"))
//////                .thenReturn(Mono.just(new Feedback()));
//////
//////        StepVerifier.create(
//////                feedbackService.submitFeedback("U1", "CITIZEN",
//////                        new FeedbackRequest("G1", 5, "ok"))
//////        )
//////        .expectError(IllegalArgumentException.class)
//////        .verify();
//////    }
//////
//////
//////
//////    @Test
//////    void getFeedbackById_success() {
//////
//////        Feedback feedback = Feedback.builder()
//////                .id("F1")
//////                .grievanceId("G1")
//////                .build();
//////
//////        when(feedbackRepository.findById("F1"))
//////                .thenReturn(Mono.just(feedback));
//////
//////        // ✅ WebClient stubbing ONLY HERE
//////        WebClient webClient = mock(WebClient.class);
//////        WebClient.RequestHeadersUriSpec uriSpec =
//////                mock(WebClient.RequestHeadersUriSpec.class);
//////        WebClient.RequestHeadersSpec headersSpec =
//////                mock(WebClient.RequestHeadersSpec.class);
//////        WebClient.ResponseSpec responseSpec =
//////                mock(WebClient.ResponseSpec.class);
//////
//////        when(webClientBuilder.build()).thenReturn(webClient);
//////        when(webClient.get()).thenReturn(uriSpec);
//////        when(uriSpec.uri(anyString(), any(Object[].class)))
//////                .thenReturn(headersSpec);
//////        when(headersSpec.header(anyString(), anyString()))
//////                .thenReturn(headersSpec);
//////        when(headersSpec.retrieve())
//////                .thenReturn(responseSpec);
//////        when(responseSpec.onStatus(any(), any()))
//////                .thenReturn(responseSpec);
//////        when(responseSpec.bodyToMono(GrievanceResponse.class))
//////                .thenReturn(Mono.just(
//////                        new GrievanceResponse("G1", "CLOSED", "U1")
//////                ));
//////
//////        StepVerifier.create(
//////                feedbackService.getFeedbackById("F1", "U1", "ADMIN")
//////        )
//////        .expectNext(feedback)
//////        .verifyComplete();
//////    }
//////}
//
package com.egov.feedback.service;

import com.egov.feedback.dto.FeedbackRequest;
import com.egov.feedback.dto.GrievanceResponse;
import com.egov.feedback.exception.AccessDeniedException;
import com.egov.feedback.model.Feedback;
import com.egov.feedback.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

	@Mock
	private FeedbackRepository feedbackRepository;

	@Mock
	private WebClient.Builder webClientBuilder;

	@Mock
	private WebClient webClient;

	@SuppressWarnings("rawtypes")
	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

	@SuppressWarnings("rawtypes")
	@Mock
	private WebClient.RequestHeadersSpec requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	@InjectMocks
	private FeedbackService feedbackService;

	private FeedbackRequest feedbackRequest;
	private GrievanceResponse closedGrievance;

	@BeforeEach
	void setUp() {
		feedbackRequest = new FeedbackRequest("GRV123", 5, "Excellent service");
		closedGrievance = GrievanceResponse.builder().id("GRV123").status("CLOSED").citizenId("USER001").build();

		when(webClientBuilder.build()).thenReturn(webClient);
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
	}

	@Test
	void submitFeedback_Success() {
		when(feedbackRepository.findByGrievanceId("GRV123")).thenReturn(Mono.empty());
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(GrievanceResponse.class)).thenReturn(Mono.just(closedGrievance));
		Feedback savedFeedback = Feedback.builder().id("FB999").build();
		when(feedbackRepository.save(any(Feedback.class))).thenReturn(Mono.just(savedFeedback));
		StepVerifier.create(feedbackService.submitFeedback("USER001", "CITIZEN", feedbackRequest)).expectNext("FB999")
				.verifyComplete();
	}

	@Test
	void submitFeedback_Fail_GrievanceNotClosed() {
		GrievanceResponse openGrievance = new GrievanceResponse("GRV123", "OPEN", "USER001");
		when(feedbackRepository.findByGrievanceId("GRV123")).thenReturn(Mono.empty());
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(GrievanceResponse.class)).thenReturn(Mono.just(openGrievance));
		StepVerifier.create(feedbackService.submitFeedback("USER001", "CITIZEN", feedbackRequest))
				.expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
						&& throwable.getMessage().contains("CLOSED grievances"))
				.verify();
	}

	@Test
	void submitFeedback_Fail_OwnershipMismatch() {
		when(feedbackRepository.findByGrievanceId("GRV123")).thenReturn(Mono.empty());
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(GrievanceResponse.class)).thenReturn(Mono.just(closedGrievance));
		StepVerifier.create(feedbackService.submitFeedback("USER_XYZ", "CITIZEN", feedbackRequest))
				.expectError(AccessDeniedException.class).verify();
	}

	@Test
	void getFeedbackByGrievanceId_Success() {
		Feedback feedback = Feedback.builder().id("FB1").grievanceId("GRV123").build();
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(GrievanceResponse.class)).thenReturn(Mono.just(closedGrievance));
		when(feedbackRepository.findByGrievanceId("GRV123")).thenReturn(Mono.just(feedback));
		StepVerifier.create(feedbackService.getFeedbackByGrievanceId("GRV123", "USER001", "CITIZEN"))
				.expectNext(feedback).verifyComplete();
	}

	@Test
	void getFeedbackById_Success() {
		Feedback feedback = Feedback.builder().id("FB1").grievanceId("GRV123").build();
		when(feedbackRepository.findById("FB1")).thenReturn(Mono.just(feedback));
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(GrievanceResponse.class)).thenReturn(Mono.just(closedGrievance));
		StepVerifier.create(feedbackService.getFeedbackById("FB1", "USER001", "CITIZEN")).expectNext(feedback)
				.verifyComplete();
	}

	@Test
	void fetchGrievance_forbidden_throwsAccessDenied() {
		when(feedbackRepository.findById("FB1"))
				.thenReturn(Mono.just(Feedback.builder().id("FB1").grievanceId("GRV123").build()));
		when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
			return responseSpec;
		});
		when(responseSpec.bodyToMono(GrievanceResponse.class))
				.thenReturn(Mono.error(new AccessDeniedException("Access denied to grievance")));
		StepVerifier.create(feedbackService.getFeedbackById("FB1", "USER001", "CITIZEN"))
				.expectError(AccessDeniedException.class).verify();
	}

	@Test
	void getFeedbackById_thenReturnFeedback_lambdaCovered() {
		Feedback feedback = Feedback.builder().id("FB2").grievanceId("GRV123").build();
		when(feedbackRepository.findById("FB2")).thenReturn(Mono.just(feedback));
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(GrievanceResponse.class)).thenReturn(Mono.just(closedGrievance));
		StepVerifier.create(feedbackService.getFeedbackById("FB2", "USER001", "CITIZEN"))
				.expectNextMatches(f -> f.getId().equals("FB2")).verifyComplete();
	}

	@Test
	void fetchGrievance_forbidden_branch_covered() {

		when(feedbackRepository.findById("FB1"))
				.thenReturn(Mono.just(Feedback.builder().id("FB1").grievanceId("GRV123").build()));

		when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> responseSpec);

		when(responseSpec.bodyToMono(GrievanceResponse.class))
				.thenReturn(Mono.error(new AccessDeniedException("Access denied to grievance")));

		StepVerifier.create(feedbackService.getFeedbackById("FB1", "USER001", "CITIZEN"))
				.expectError(AccessDeniedException.class).verify();
	}

	@Test
	void fetchGrievance_notFound_branch_covered() {
		when(feedbackRepository.findById("FB2"))
				.thenReturn(Mono.just(Feedback.builder().id("FB2").grievanceId("GRV404").build()));
		when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> responseSpec);
		when(responseSpec.bodyToMono(GrievanceResponse.class))
				.thenReturn(Mono.error(new IllegalArgumentException("Grievance not found")));
		StepVerifier.create(feedbackService.getFeedbackById("FB2", "USER001", "CITIZEN"))
				.expectError(IllegalArgumentException.class).verify();
	}

}