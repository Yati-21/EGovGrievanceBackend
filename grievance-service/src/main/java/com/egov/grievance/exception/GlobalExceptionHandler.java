package com.egov.grievance.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String ERROR = "error";

	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<Map<String, String>> handleValidation(WebExchangeBindException ex) {

		Map<String, String> errors = new HashMap<>();
		ex.getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegal(IllegalArgumentException ex) {

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(ERROR, ex.getMessage()));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(ERROR, ex.getMessage()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode())
				.body(Map.of(ERROR, ex.getReason() != null ? ex.getReason() : "Error occurred"));
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ResponseEntity<Map<String, String>> handleServiceDown(ServiceUnavailableException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(ERROR, ex.getMessage()));
	}
}
