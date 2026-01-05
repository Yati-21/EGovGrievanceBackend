//package com.egov.feedback.exception;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.support.WebExchangeBindException;
//
//import java.util.Map;
//
//class GlobalExceptionHandlerTest {
//
//    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
//
//    @Test
//    void handleIllegalArgument() {
//        ResponseEntity<Map<String, String>> res =
//                handler.handleIllegalArgument(new IllegalArgumentException("bad"));
//
//        assertEquals(400, res.getStatusCode().value());
//    }
//
//    @Test
//    void handleAccessDenied() {
//        ResponseEntity<Map<String, String>> res =
//                handler.handleAccessDenied(new AccessDeniedException("denied"));
//
//        assertEquals(403, res.getStatusCode().value());
//    }
//
//    @Test
//    void handleGeneric() {
//        ResponseEntity<Map<String, String>> res =
//                handler.handleGeneric(new RuntimeException("oops"));
//
//        assertEquals(500, res.getStatusCode().value());
//    }
//}
package com.egov.feedback.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleIllegalArgument() {
		ResponseEntity<Map<String, String>> response = handler
				.handleIllegalArgument(new IllegalArgumentException("bad"));
		assertEquals(400, response.getStatusCode().value());
		assertEquals("bad", response.getBody().get("error"));
	}

	@Test
	void handleAccessDenied() {
		ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(new AccessDeniedException("denied"));
		assertEquals(403, response.getStatusCode().value());
		assertEquals("denied", response.getBody().get("error"));
	}

	@Test
	void handleGeneric() {
		ResponseEntity<Map<String, String>> response = handler.handleGeneric(new RuntimeException("oops"));
		assertEquals(500, response.getStatusCode().value());
		assertTrue(response.getBody().get("error").contains("unexpected error occurred"));
	}

	@Test
	void handleValidationExceptions() {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "feedbackRequest");
		bindingResult.addError(new FieldError("feedbackRequest", "grievanceId", "Grievance ID is mandatory"));
		bindingResult.addError(new FieldError("feedbackRequest", "rating", "Rating must be between 1 and 5"));
		MethodParameter methodParameter = new MethodParameter(this.getClass().getDeclaredMethods()[0], -1);
		WebExchangeBindException ex = new WebExchangeBindException(methodParameter, bindingResult);
		ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);
		assertEquals(400, response.getStatusCode().value());
		assertEquals("Grievance ID is mandatory", response.getBody().get("grievanceId"));
		assertEquals("Rating must be between 1 and 5", response.getBody().get("rating"));
	}
}
