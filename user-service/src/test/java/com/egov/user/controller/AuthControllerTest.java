package com.egov.user.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.egov.user.dto.LoginRequest;
import com.egov.user.dto.LoginResponse;
import com.egov.user.dto.RegisterRequest;
import com.egov.user.service.UserService;

import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AuthController.class,
    properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
    }
)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("test@test.com");
        request.setPassword("StrongPass1!");

        Mockito.when(userService.register(Mockito.any()))
                .thenReturn(Mono.just("user123"));

        webTestClient.post()
                .uri("/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).isEqualTo("user123");
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("StrongPass1!");

        Mockito.when(userService.login(Mockito.any()))
                .thenReturn(Mono.just(new LoginResponse("token","id","ADMIN")));

        webTestClient.post()
                .uri("/auth/login")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void login_validation_error() {
        LoginRequest request = new LoginRequest();

        webTestClient.post()
                .uri("/auth/login")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

}
