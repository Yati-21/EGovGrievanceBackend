package com.egov.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.egov.user.model.ROLE;
import com.egov.user.model.User;
import com.egov.user.repository.UserRepository;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // INTERNAL API
    // Used by grievance-service during escalation
    @GetMapping("/supervisor/department/{departmentId}")
    public Mono<String> getSupervisorByDepartment(
            @PathVariable String departmentId) {

        return userRepository
                .findByRoleAndDepartmentId(ROLE.SUPERVISOR, departmentId)
                .map(User::getId)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException(
                                "Supervisor not found for department " + departmentId)));
    }

    @GetMapping("/{id}")
    public Mono<com.egov.user.dto.UserResponse> getUserById(@PathVariable String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new com.egov.user.exception.ResourceNotFoundException("User not found")))
                .map(user -> com.egov.user.dto.UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .departmentId(user.getDepartmentId())
                        .build());
    }
    
}
