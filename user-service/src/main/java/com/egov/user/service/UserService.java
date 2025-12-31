package com.egov.user.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatusCode;

import com.egov.user.dto.LoginRequest;
import com.egov.user.dto.LoginResponse;
import com.egov.user.dto.RegisterRequest;
import com.egov.user.exception.UserAlreadyExistsException;
import com.egov.user.model.ROLE;
import com.egov.user.model.User;
import com.egov.user.repository.UserRepository;
import com.egov.user.security.JwtUtil;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WebClient.Builder webClientBuilder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            WebClient.Builder webClientBuilder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<String> register(RegisterRequest request) {

        return userRepository.findByEmail(request.getEmail())
                .flatMap(existing -> Mono.<User>error(new UserAlreadyExistsException("Email already registered")))
                .switchIfEmpty(
                        Mono.defer(() -> {

                            ROLE role = resolveRole(request.getRole());

                            return validateDepartmentRuleReactive(role, request.getDepartmentId())
                                    .then(validateDepartmentFromGrievanceService(role, request.getDepartmentId()))
                                    .then(Mono.fromCallable(() -> {

                                        return User.builder()
                                                .name(request.getName())
                                                .email(request.getEmail())
                                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                                .role(role)
                                                .departmentId(request.getDepartmentId())
                                                .createdAt(Instant.now())
                                                .build();
                                    }))
                                    .flatMap(user -> userRepository.save(user));

                        }))
                .map(User::getId);
    }

    public Mono<LoginResponse> login(LoginRequest request) {

        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Email not found")))
                .flatMap(user -> {

                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new IllegalArgumentException("Incorrect Password"));
                    }

                    String token = jwtUtil.generateToken(
                            user.getId(),
                            user.getRole().name());

                    return Mono.just(
                            new LoginResponse(
                                    token,
                                    user.getId(),
                                    user.getRole().name()));
                });
    }

    @PostConstruct
    public void createDefaultAdmin() {
        userRepository.findByEmail("admin@egov.com")
                .switchIfEmpty(Mono.defer(() -> {
                    User admin = User.builder()
                            .name("Admin")
                            .email("admin@egov.com")
                            .passwordHash(passwordEncoder.encode("Admin@123"))
                            .role(ROLE.ADMIN)
                            .createdAt(Instant.now())
                            .build();
                    return userRepository.save(admin);
                }))
                .subscribe();
    }

    private ROLE resolveRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return ROLE.CITIZEN;
        }

        try {
            return ROLE.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role provided");
        }
    }

    private Mono<Void> validateDepartmentRuleReactive(ROLE role, String departmentId) {

        if ((role == ROLE.OFFICER || role == ROLE.SUPERVISOR)
                && (departmentId == null || departmentId.isBlank())) {
            return Mono.error(new IllegalArgumentException("departmentId is mandatory for OFFICER and SUPERVISOR"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateDepartmentFromGrievanceService(
            ROLE role,
            String departmentId) {

        // Only OFFICER & SUPERVISOR need department validation
        if (role == ROLE.OFFICER || role == ROLE.SUPERVISOR) {
            return webClientBuilder.build()
                    .get()
                    .uri("http://grievance-service/reference/departments/{id}/validate", departmentId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(Map.class)
                            .flatMap(error -> Mono.error(new IllegalArgumentException(
                                    (String) error.getOrDefault("error", "Invalid department")))))
                    .bodyToMono(Void.class);
        }

        return Mono.empty();
    }

}