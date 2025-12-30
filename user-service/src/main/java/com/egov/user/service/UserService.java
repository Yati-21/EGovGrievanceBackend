package com.egov.user.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.egov.user.dto.RegisterRequest;
import com.egov.user.model.ROLE;
import com.egov.user.model.User;
import com.egov.user.repository.UserRepository;

import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<String> register(RegisterRequest request) {

        return userRepository.findByEmail(request.getEmail())
                .flatMap(existing -> Mono.<User>error(new IllegalStateException("Email already registered")))
                .switchIfEmpty(
                        Mono.defer(() -> {
                        	
                            ROLE role = resolveRole(request.getRole());
                            validateDepartmentRule(role, request.getDepartmentId());

                            User user = User.builder()
                                    .name(request.getName())
                                    .email(request.getEmail())
                                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                                    .role(role)
                                    .departmentId(request.getDepartmentId())
                                    .createdAt(Instant.now())
                                    .build();

                            return userRepository.save(user);
                        }))
                .map(User::getId);
    }

    private ROLE resolveRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return ROLE.CITIZEN;
        }
        return ROLE.valueOf(roleValue.toUpperCase());
    }

    private void validateDepartmentRule(ROLE role, String departmentId) {

        if ((role == ROLE.OFFICER || role == ROLE.SUPERVISOR) && (departmentId == null || departmentId.isBlank())) {
            throw new IllegalArgumentException("departmentId is mandatory for OFFICER and SUPERVISOR");
        }
    }
}