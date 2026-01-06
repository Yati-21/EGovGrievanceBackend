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
import com.egov.user.dto.UserResponse;
import com.egov.user.dto.UserUpdateRequest;
import com.egov.user.exception.ForbiddenException;
import com.egov.user.exception.ResourceNotFoundException;
import com.egov.user.exception.UserAlreadyExistsException;
import com.egov.user.model.ROLE;
import com.egov.user.model.User;
import com.egov.user.repository.UserRepository;
import com.egov.user.security.JwtUtil;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
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
    

	private static final String USER_NOT_FOUND = "User not found";

    public Mono<String> register(RegisterRequest request) {

        return userRepository.findByEmail(request.getEmail())
                .flatMap(existing -> Mono.<User>error(new UserAlreadyExistsException("Email already registered")))
                .switchIfEmpty(
                        Mono.defer(() -> {

                            ROLE role = resolveRole(request.getRole());
                            if (role == ROLE.CITIZEN) {
                                request.setDepartmentId(null); // citizen cant have department
                            }

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
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            response -> Mono.error(new IllegalArgumentException("Invalid department")))
                    .bodyToMono(Void.class);
        }
        return Mono.empty();
    }

    

    public Mono<UserResponse> updateProfile(
            String targetUserId,
            UserUpdateRequest request,
            String loggedInUserId,
            ROLE loggedInUserRole)
    {
        if (!targetUserId.equals(loggedInUserId)) {
            return Mono.error(new ForbiddenException(
                    "You cannot update another user's profile"));
        }
        return userRepository.findById(targetUserId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(USER_NOT_FOUND)))
                .flatMap(user -> {
                    if (request.getName() != null && !request.getName().isBlank()) {
                        if (user.getName().equals(request.getName())) {
                            return Mono.error(new IllegalArgumentException("New name cannot be same as existing name"));
                        }
                        user.setName(request.getName());
                    }
                    if (request.getEmail() != null && !request.getEmail().isBlank()) {
                        if (user.getEmail().equals(request.getEmail())) {
                            return Mono
                                    .error(new IllegalArgumentException("New email cannot be same as existing email"));
                        }
                        return userRepository.findByEmail(request.getEmail())
                                .filter(u -> !u.getId().equals(targetUserId))
                                .flatMap(existing -> Mono.<User>error(new UserAlreadyExistsException("Email already in use")))
                                .switchIfEmpty(Mono.just(user))
                                .map(u -> {
                                    u.setEmail(request.getEmail());
                                    return u;
                                });
                    }
                    return Mono.just(user);
                })
                .flatMap(userRepository::save)
                .map(this::mapToResponse);
    }

    public Mono<Void> updateRole(
            String targetUserId,
            String roleName,
            String loggedInUserId,
            ROLE loggedInUserRole)
 {
        if (loggedInUserRole != ROLE.ADMIN) {
            return Mono.error(new ForbiddenException(
                    "Only ADMIN can update user roles"));
        }

        ROLE newRole = resolveRole(roleName);

        return userRepository.findById(targetUserId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(USER_NOT_FOUND)))
                .flatMap(user -> {
                    if (user.getRole() == newRole) {
                        return Mono.error(new IllegalArgumentException("New role cannot be same as current role"));
                    }
                    user.setRole(newRole);
                    if (newRole == ROLE.CITIZEN) {
                        user.setDepartmentId(null);
                    }
                    return userRepository.save(user);
                })
                .then();
    }

    public Mono<Void> updateDepartment(
            String targetUserId,
            String departmentId,
            String loggedInUserId,
            ROLE loggedInUserRole)
 {
        if (loggedInUserRole != ROLE.ADMIN) {
            return Mono.error(new ForbiddenException(
                    "Only ADMIN can assign departments"));
        }

        return userRepository.findById(targetUserId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(USER_NOT_FOUND)))
                .flatMap(user -> {
                    if (user.getRole() == ROLE.CITIZEN) {
                        return Mono.error(new IllegalArgumentException(
                                "Citizen cannot be assigned a department"));
                    }
                    if (departmentId.equals(user.getDepartmentId())) {
                        return Mono.error(
                                new IllegalArgumentException("New department cannot be same as current department"));
                    }
                    return validateDepartmentFromGrievanceService(user.getRole(), departmentId)
                            .then(Mono.defer(() -> {
                                user.setDepartmentId(departmentId);
                                return userRepository.save(user);
                            }));
                })
                .then();
    }

    public Flux<UserResponse> getUsersByRole(
            String roleName,
            ROLE loggedInUserRole)
	{
        if (loggedInUserRole != ROLE.ADMIN) {
            return Flux.error(new ForbiddenException(
                    "Only ADMIN can view users by role"));
        }

        ROLE role = resolveRole(roleName);
        return userRepository.findByRole(role)
                .map(this::mapToResponse);
    }

    public Flux<UserResponse> getOfficersByDepartment(
            String departmentId,
            String loggedInUserId,
            ROLE loggedInUserRole) {

        // ADMIN can see all officers in any dept
        // SUPERVISOR can only see officers in their dept
        if (loggedInUserRole == ROLE.ADMIN) {
            return userRepository.findByRoleAndDepartmentId(ROLE.OFFICER, departmentId)
                    .map(this::mapToResponse);
        } else if (loggedInUserRole == ROLE.SUPERVISOR) {
            return userRepository.findById(loggedInUserId)
                    .flatMapMany(supervisor -> {
                        if (supervisor.getDepartmentId() == null
                                || !supervisor.getDepartmentId().equals(departmentId)) {
                            return Flux.error(new ForbiddenException(
                                    "Supervisor can only view officers of their own department"));
                        }
                        return userRepository.findByRoleAndDepartmentId(ROLE.OFFICER, departmentId)
                                .map(this::mapToResponse);
                    });
        }
        return Flux.error(new ForbiddenException("Access denied"));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(user.getDepartmentId())
                .build();
    }
}