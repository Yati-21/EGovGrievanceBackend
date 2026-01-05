package com.egov.reporting.client;

import com.egov.reporting.dto.GrievanceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrievanceClientTest {

	@Mock
	private WebClient.Builder webClientBuilder;

	@Mock
	private WebClient webClient;

	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

	@Mock
	private WebClient.RequestHeadersSpec requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	private GrievanceClient grievanceClient;

	@BeforeEach
	void setUp() {
		when(webClientBuilder.build()).thenReturn(webClient);
		grievanceClient = new GrievanceClient(webClientBuilder);
	}

	@Test
	void getGrievances_Success() {
		GrievanceDTO dto = new GrievanceDTO();
		dto.setId("123");
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToFlux(GrievanceDTO.class)).thenReturn(Flux.just(dto));
		StepVerifier.create(grievanceClient.getGrievances("user1", "ADMIN", "OPEN", "DEPT1"))
				.expectNextMatches(result -> result.getId().equals("123")).verifyComplete();
	}

	@Test
	void getSlaBreaches_Success() {
		GrievanceDTO dto = new GrievanceDTO();
		dto.setId("SLA-123");

		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToFlux(GrievanceDTO.class)).thenReturn(Flux.just(dto));

		StepVerifier.create(grievanceClient.getSlaBreaches("user1", "ADMIN"))
				.expectNextMatches(result -> result.getId().equals("SLA-123")).verifyComplete();
	}

	@Test
	void getGrievances_WithFilters_Success() {
		GrievanceDTO dto = new GrievanceDTO();
		dto.setId("456");
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToFlux(GrievanceDTO.class)).thenReturn(Flux.just(dto));
		StepVerifier.create(grievanceClient.getGrievances("user1", "ADMIN", "OPEN", "DEPT1"))
				.expectNextMatches(result -> result.getId().equals("456")).verifyComplete();
	}

	@Test
	void getGrievances_WithNullFilters_Success() {
		GrievanceDTO dto = new GrievanceDTO();

		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToFlux(GrievanceDTO.class)).thenReturn(Flux.just(dto));
		StepVerifier.create(grievanceClient.getGrievances("user1", "ADMIN", null, null)).expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void getGrievances_uriBuilder_lambda_covered() {
		GrievanceDTO dto = new GrievanceDTO();
		dto.setId("789");
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		ArgumentCaptor<Function<org.springframework.web.util.UriBuilder, java.net.URI>> captor = ArgumentCaptor
				.forClass(Function.class);
		when(requestHeadersUriSpec.uri(captor.capture())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToFlux(GrievanceDTO.class)).thenReturn(Flux.just(dto));
		StepVerifier.create(grievanceClient.getGrievances("user1", "ADMIN", "OPEN", "DEPT1")).expectNextCount(1)
				.verifyComplete();
		org.springframework.web.util.DefaultUriBuilderFactory factory = new org.springframework.web.util.DefaultUriBuilderFactory();
		captor.getValue().apply(factory.builder());
	}

}