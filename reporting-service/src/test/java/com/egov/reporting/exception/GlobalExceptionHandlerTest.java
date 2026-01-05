package com.egov.reporting.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleWebClientError() {
		WebClientResponseException ex = new WebClientResponseException(404, "Not Found", null, "error".getBytes(),
				StandardCharsets.UTF_8);
		ResponseEntity<String> res = handler.handleWebClientError(ex);
		assertEquals(404, res.getStatusCode().value());
	}

	@Test
	void handleResponseStatusException() {
		ResponseStatusException ex = new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
				"bad");
		ResponseEntity<Map<String, String>> res = handler.handleResponseStatusException(ex);
		assertEquals(400, res.getStatusCode().value());
	}

	@Test
	void handleIllegalArgument() {
		ResponseEntity<Map<String, String>> res = handler.handleIllegalArgument(new IllegalArgumentException("bad"));
		assertEquals(400, res.getStatusCode().value());
	}

	@Test
	void handleGeneric() {
		ResponseEntity<Map<String, String>> res = handler.handleGeneric(new RuntimeException("oops"));
		assertEquals(500, res.getStatusCode().value());
	}
}
