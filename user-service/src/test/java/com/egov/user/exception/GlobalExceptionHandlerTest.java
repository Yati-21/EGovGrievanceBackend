//package com.egov.user.exception;
//
//import com.egov.user.controller.AuthController;
//import com.egov.user.service.UserService;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.reactive.server.WebTestClient;
//
//@WebFluxTest(
//    controllers = AuthController.class,
//    properties = {
//        "spring.cloud.config.enabled=false"
//    }
//)
//@Import(GlobalExceptionHandler.class)
//class GlobalExceptionHandlerTest {
//
//    @Autowired
//    WebTestClient webTestClient;
//
//    @MockBean
//    UserService userService;
//
//    @Test
//    void validation_error_on_login() {
//        webTestClient.post()
//                .uri("/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue("{}")
//                .exchange()
//                .expectStatus().isBadRequest();
//    }
//}
package com.egov.user.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

@ExtendWith(SpringExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();


    @Test
    void handleValidationExceptions() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");

        bindingResult.addError(
                new FieldError("request", "email", "Invalid email")
        );

        WebExchangeBindException ex =
                new WebExchangeBindException(null, bindingResult);

        ResponseEntity<Map<String, String>> response =
                handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid email", response.getBody().get("email"));
    }


    @Test
    void handleUserAlreadyExists() {
        UserAlreadyExistsException ex =
                new UserAlreadyExistsException("Email already registered");

        ResponseEntity<Map<String, String>> response =
                handler.handleUserAlreadyExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already registered", response.getBody().get("error"));
    }


    @Test
    void handleIllegalArgument() {
        IllegalArgumentException ex =
                new IllegalArgumentException("Invalid input");

        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().get("error"));
    }


    @Test
    void handleResourceNotFound() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("User not found");

        ResponseEntity<Map<String, String>> response =
                handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().get("error"));
    }


    @Test
    void handleForbidden() {
        ForbiddenException ex =
                new ForbiddenException("Access denied");

        ResponseEntity<Map<String, String>> response =
                handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().get("error"));
    }
}
