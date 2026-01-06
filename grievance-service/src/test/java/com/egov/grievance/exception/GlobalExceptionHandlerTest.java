package com.egov.grievance.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleIllegalArgument() {
		ResponseEntity<Map<String, String>> res = handler.handleIllegal(new IllegalArgumentException("bad"));
		assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
		assertEquals("bad", res.getBody().get("error"));
	}

	@Test
	void handleServiceUnavailable() {
		ResponseEntity<Map<String, String>> res = handler
				.handleServiceDown(new ServiceUnavailableException("service down"));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
		assertEquals("service down", res.getBody().get("error"));
	}

	@Test
	void handleUserNotFound() {
		ResponseEntity<Map<String, String>> res = handler.handleUserNotFound(new UserNotFoundException("user missing"));
		assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
		assertEquals("user missing", res.getBody().get("error"));
	}

	@Test
	void handleResponseStatus_withReason() {
		ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "denied");
		ResponseEntity<Map<String, String>> res = handler.handleResponseStatus(ex);
		assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
		assertEquals("denied", res.getBody().get("error"));
	}

	@Test
	void handleResponseStatus_withoutReason() {
		ResponseStatusException ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		ResponseEntity<Map<String, String>> res = handler.handleResponseStatus(ex);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
		assertEquals("Error occurred", res.getBody().get("error"));
	}

	@Test
	void handleValidation() {
		BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "title", "must not be blank"));
		WebExchangeBindException ex = new WebExchangeBindException(null, bindingResult);
		ResponseEntity<Map<String, String>> res = handler.handleValidation(ex);
		assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
		assertEquals("must not be blank", res.getBody().get("title"));
	}
}
