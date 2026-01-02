package com.egov.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.egov.user.dto.UserResponse;
import com.egov.user.dto.UserUpdateRequest;
import com.egov.user.model.ROLE;
import com.egov.user.model.User;
import com.egov.user.repository.UserRepository;
import com.egov.user.service.UserService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
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
    public Mono<UserResponse> getUserById(@PathVariable String id) {
    	
    	//--to check load balancing
    	System.out.println("User Service Instance received request for User ID: " + id);
    	
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new com.egov.user.exception.ResourceNotFoundException("User not found")))
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .departmentId(user.getDepartmentId())
                        .build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal")
    public Mono<UserResponse> updateProfile(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return userService.updateProfile(userId, request);
    }

    @PutMapping("/{userId}/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserResponse> updateUserRole(@PathVariable String userId, @PathVariable String role) {
        return userService.updateRole(userId, role);
    }

    @PutMapping("/{userId}/department/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserResponse> assignDepartment(@PathVariable String userId, @PathVariable String departmentId) {
        return userService.updateDepartment(userId, departmentId);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<UserResponse> getUsersByRole(@PathVariable String role) {
        return userService.getUsersByRole(role);
    }
}
