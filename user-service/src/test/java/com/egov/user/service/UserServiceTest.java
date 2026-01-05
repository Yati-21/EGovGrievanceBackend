package com.egov.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import com.egov.user.dto.LoginRequest;
import com.egov.user.dto.LoginResponse;
import com.egov.user.dto.RegisterRequest;
import com.egov.user.dto.UserUpdateRequest;
import com.egov.user.exception.ForbiddenException;
import com.egov.user.exception.ResourceNotFoundException;
import com.egov.user.exception.UserAlreadyExistsException;
import com.egov.user.model.ROLE;
import com.egov.user.model.User;
import com.egov.user.repository.UserRepository;
import com.egov.user.security.JwtUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id("u1")
                .name("Test")
                .email("test@test.com")
                .passwordHash("hashed")
                .role(ROLE.CITIZEN)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void register_success_citizen() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test");
        req.setEmail("test@test.com");
        req.setPassword("pass123");

        when(userRepository.findByEmail(req.getEmail()))
                .thenReturn(Mono.empty());
        when(passwordEncoder.encode(any()))
                .thenReturn("hashed");
        when(userRepository.save(any()))
                .thenReturn(Mono.just(user));

        StepVerifier.create(userService.register(req))
                .expectNext("u1")
                .verifyComplete();
    }

    @Test
    void register_emailAlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@test.com");

        when(userRepository.findByEmail(req.getEmail()))
                .thenReturn(Mono.just(user));

        StepVerifier.create(userService.register(req))
                .expectError(UserAlreadyExistsException.class)
                .verify();
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("pass123");

        when(userRepository.findByEmail(req.getEmail()))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(true);
        when(jwtUtil.generateToken("u1", "CITIZEN"))
                .thenReturn("token");

        StepVerifier.create(userService.login(req))
                .assertNext(res -> {
                    assertEquals("token", res.getToken());
                    assertEquals("u1", res.getUserId());
                })
                .verifyComplete();
    }

    @Test
    void login_wrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("wrong");

        when(userRepository.findByEmail(req.getEmail()))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);

        StepVerifier.create(userService.login(req))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void updateProfile_success() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setName("New Name");

        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(user));
        when(userRepository.save(any()))
                .thenReturn(Mono.just(user));

        StepVerifier.create(
                userService.updateProfile("u1", req, "u1", ROLE.CITIZEN)
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void updateProfile_forbidden() {
        StepVerifier.create(
                userService.updateProfile("u1", new UserUpdateRequest(), "u2", ROLE.CITIZEN)
        )
        .expectError(ForbiddenException.class)
        .verify();
    }

    @Test
    void updateRole_adminSuccess() {
        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(user));
        when(userRepository.save(any()))
                .thenReturn(Mono.just(user));

        StepVerifier.create(
                userService.updateRole("u1", "OFFICER", "admin", ROLE.ADMIN)
        )
        .verifyComplete();
    }

    @Test
    void updateRole_nonAdminForbidden() {
        StepVerifier.create(
                userService.updateRole("u1", "OFFICER", "u2", ROLE.CITIZEN)
        )
        .expectError(ForbiddenException.class)
        .verify();
    }

    @Test
    void getUsersByRole_admin() {
        when(userRepository.findByRole(ROLE.OFFICER))
                .thenReturn(Flux.just(user));

        StepVerifier.create(
                userService.getUsersByRole("OFFICER", ROLE.ADMIN)
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void getUsersByRole_forbidden() {
        StepVerifier.create(
                userService.getUsersByRole("OFFICER", ROLE.CITIZEN)
        )
        .expectError(ForbiddenException.class)
        .verify();
    }

    @Test
    void getOfficersByDepartment_admin() {
        when(userRepository.findByRoleAndDepartmentId(eq(ROLE.OFFICER), eq("D1")))
                .thenReturn(Flux.just(user));

        StepVerifier.create(
                userService.getOfficersByDepartment("D1", "admin", ROLE.ADMIN)
        )
        .expectNextCount(1)
        .verifyComplete();
    }
    
//    @Test
//    void resolveRole_blank_returnsCitizen() {
//        StepVerifier.create(userService.getUsersByRole("", ROLE.CITIZEN))
//                .expectError(IllegalArgumentException.class)
//                .verify();
//    }
    @Test
    void login_emailNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail(req.getEmail()))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.login(req))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
//    @Test
//    void register_officer_withDepartment_success() {
//        RegisterRequest req = new RegisterRequest();
//        req.setName("Officer");
//        req.setEmail("officer@test.com");
//        req.setPassword("pass");
//        req.setRole("OFFICER");
//        req.setDepartmentId("D001");
//
//        when(userRepository.findByEmail(req.getEmail()))
//                .thenReturn(Mono.empty());
//        when(passwordEncoder.encode(any()))
//                .thenReturn("hashed");
//        when(userRepository.save(any()))
//                .thenReturn(Mono.just(user));
//
//        // mock WebClient validation
//        WebClient webClient = WebClient.builder().build();
//        when(webClientBuilder.build()).thenReturn(webClient);
//
//        StepVerifier.create(userService.register(req))
//                .expectNext("u1")
//                .verifyComplete();
//    }
//    @Test
//    void register_officer_withoutDepartment_fails() {
//        RegisterRequest req = new RegisterRequest();
//        req.setName("Officer");
//        req.setEmail("officer@test.com");
//        req.setPassword("pass");
//        req.setRole("CITIZEN");
//
//        when(userRepository.findByEmail(req.getEmail()))
//                .thenReturn(Mono.empty());
//
//        StepVerifier.create(userService.register(req))
//                .expectError(IllegalArgumentException.class)
//                .verify();
//    }
    @Test
    void updateDepartment_citizenForbidden() {
        user.setRole(ROLE.CITIZEN);

        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(user));

        StepVerifier.create(
                userService.updateDepartment("u1", "D1", "admin", ROLE.ADMIN)
        )
        .expectError(IllegalArgumentException.class)
        .verify();
    }
    @Test
    void updateDepartment_sameDepartment_fails() {
        user.setRole(ROLE.OFFICER);
        user.setDepartmentId("D1");

        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(user));

        StepVerifier.create(
                userService.updateDepartment("u1", "D1", "admin", ROLE.ADMIN)
        )
        .expectError(IllegalArgumentException.class)
        .verify();
    }
    @Test
    void getOfficersByDepartment_supervisorSuccess() {
        user.setRole(ROLE.SUPERVISOR);
        user.setDepartmentId("D1");

        when(userRepository.findById("s1"))
                .thenReturn(Mono.just(user));
        when(userRepository.findByRoleAndDepartmentId(ROLE.OFFICER, "D1"))
                .thenReturn(Flux.just(user));

        StepVerifier.create(
                userService.getOfficersByDepartment("D1", "s1", ROLE.SUPERVISOR)
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void getOfficersByDepartment_supervisorWrongDept() {
        user.setRole(ROLE.SUPERVISOR);
        user.setDepartmentId("D2");

        when(userRepository.findById("s1"))
                .thenReturn(Mono.just(user));

        StepVerifier.create(
                userService.getOfficersByDepartment("D1", "s1", ROLE.SUPERVISOR)
        )
        .expectError(ForbiddenException.class)
        .verify();
    }
    @Test
    void getOfficersByDepartment_citizenForbidden() {
        StepVerifier.create(
                userService.getOfficersByDepartment("D1", "u1", ROLE.CITIZEN)
        )
        .expectError(ForbiddenException.class)
        .verify();
    }
    @Test
    void updateProfile_sameNameFails() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setName(user.getName());

        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(user));

        StepVerifier.create(
                userService.updateProfile("u1", req, "u1", ROLE.CITIZEN)
        )
        .expectError(IllegalArgumentException.class)
        .verify();
    }
    @Test
    void updateProfile_emailAlreadyExists() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("new@test.com");

        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(user));
        when(userRepository.findByEmail("new@test.com"))
                .thenReturn(Mono.just(User.builder().id("u2").build()));

        StepVerifier.create(
                userService.updateProfile("u1", req, "u1", ROLE.CITIZEN)
        )
        .expectError(UserAlreadyExistsException.class)
        .verify();
    }
    @Test
    void updateRole_sameRoleFails() {
        user.setRole(ROLE.OFFICER);

        when(userRepository.findById("u1"))
                .thenReturn(Mono.just(user));

        StepVerifier.create(
                userService.updateRole("u1", "OFFICER", "admin", ROLE.ADMIN)
        )
        .expectError(IllegalArgumentException.class)
        .verify();
    }

}
