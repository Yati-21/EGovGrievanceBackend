package com.egov.reporting.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClientTest {

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

	private UserClient userClient;

	@BeforeEach
	void setUp() {
		when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
		when(webClientBuilder.build()).thenReturn(webClient);

		userClient = new UserClient(webClientBuilder);
	}

	@Test
	void validateUserExists_ReturnsTrue_On2xx() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));
		StepVerifier.create(userClient.validateUserExists("user1", "req1", "ADMIN")).expectNext(true).verifyComplete();
	}

	@Test
	void validateUserExists_ReturnsFalse_OnError() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.toBodilessEntity()).thenReturn(Mono.error(new RuntimeException("Service Down")));
		StepVerifier.create(userClient.validateUserExists("user1", "req1", "ADMIN")).expectNext(false).verifyComplete();
	}
}