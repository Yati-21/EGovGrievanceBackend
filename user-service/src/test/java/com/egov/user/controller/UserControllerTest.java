package com.egov.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.egov.user.dto.UserResponse;
import com.egov.user.dto.UserUpdateRequest;
import com.egov.user.exception.ResourceNotFoundException;
import com.egov.user.model.ROLE;
import com.egov.user.repository.UserRepository;
import com.egov.user.service.UserService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(
    controllers = UserController.class,
    properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
    }
)
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getUserById_success() {
        UserResponse response = UserResponse.builder()
                .id("u1")
                .name("Test User")
                .email("test@test.com")
                .role(ROLE.CITIZEN)
                .build();

        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(
                        com.egov.user.model.User.builder()
                                .id("u1")
                                .name("Test User")
                                .email("test@test.com")
                                .role(ROLE.CITIZEN)
                                .build()
                ));

        webTestClient.get()
                .uri("/users/u1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("u1")
                .jsonPath("$.email").isEqualTo("test@test.com");
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById("missing"))
                .thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/users/missing")
                .exchange()
                .expectStatus().isNotFound();
    }


    @Test
    void updateProfile_success() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated");

        when(userService.updateProfile(
                eq("u1"),
                any(),
                eq("u1"),
                eq(ROLE.CITIZEN)
        )).thenReturn(Mono.just(
                UserResponse.builder()
                        .id("u1")
                        .name("Updated")
                        .build()
        ));

        webTestClient.put()
                .uri("/users/u1")
                .header("X-USER-ID", "u1")
                .header("X-USER-ROLE", "CITIZEN")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateProfile_forbidden() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Hacker");

        when(userService.updateProfile(
                eq("u1"),
                any(),
                eq("u2"),
                eq(ROLE.CITIZEN)
        )).thenReturn(Mono.error(
                new com.egov.user.exception.ForbiddenException("Forbidden")
        ));

        webTestClient.put()
                .uri("/users/u1")
                .header("X-USER-ID", "u2")
                .header("X-USER-ROLE", "CITIZEN")
                .bodyValue(request)
                .exchange()
                .expectStatus().isForbidden();
    }


    @Test
    void updateUserRole_adminOnly_success() {
        when(userService.updateRole(
                eq("u2"),
                eq("OFFICER"),
                eq("admin"),
                eq(ROLE.ADMIN)
        )).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/users/u2/role/OFFICER")
                .header("X-USER-ID", "admin")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateUserRole_forbidden() {
        when(userService.updateRole(
                any(), any(), any(), eq(ROLE.CITIZEN)
        )).thenReturn(Mono.error(
                new com.egov.user.exception.ForbiddenException("Only ADMIN")
        ));

        webTestClient.put()
                .uri("/users/u2/role/OFFICER")
                .header("X-USER-ID", "u1")
                .header("X-USER-ROLE", "CITIZEN")
                .exchange()
                .expectStatus().isForbidden();
    }


    @Test
    void getUsersByRole_adminSuccess() {
        when(userService.getUsersByRole("OFFICER", ROLE.ADMIN))
                .thenReturn(Flux.just(
                        UserResponse.builder().id("u1").build(),
                        UserResponse.builder().id("u2").build()
                ));

        webTestClient.get()
                .uri("/users/role/OFFICER")
                .header("X-USER-ID", "admin")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void getOfficersByDepartment_adminSuccess() {
        when(userService.getOfficersByDepartment(
                eq("D1"), eq("admin"), eq(ROLE.ADMIN)))
                .thenReturn(Flux.just(
                        UserResponse.builder().id("o1").build()
                ));

        webTestClient.get()
                .uri("/users/department/D1/officers")
                .header("X-USER-ID", "admin")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk();
    }
}
