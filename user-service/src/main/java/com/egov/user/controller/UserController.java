package com.egov.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public Mono<UserResponse> getUserById(@PathVariable String id) {

    	//--to check load balancing
    	//System.out.println("User Service Instance received request for User ID: " + id);

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
    public Mono<Void> updateProfile(@PathVariable String userId, @RequestBody UserUpdateRequest request,
            @RequestHeader("X-USER-ID") String loggedInUserId,
            @RequestHeader("X-USER-ROLE") String loggedInUserRole) {
        return userService.updateProfile(userId, request, loggedInUserId, ROLE.valueOf(loggedInUserRole)).then();
    }

    @PutMapping("/{userId}/role/{role}")
    public Mono<Void> updateUserRole(@PathVariable String userId, @PathVariable String role,
            @RequestHeader("X-USER-ID") String loggedInUserId, @RequestHeader("X-USER-ROLE") String loggedInUserRole) {
        return userService.updateRole(userId, role, loggedInUserId, ROLE.valueOf(loggedInUserRole));
    }

    @PutMapping("/{userId}/department/{departmentId}")
    public Mono<Void> assignDepartment(@PathVariable String userId, @PathVariable String departmentId ,@RequestHeader("X-USER-ID") String loggedInUserId,@RequestHeader("X-USER-ROLE") String loggedInUserRole) {
    	return userService.updateDepartment(userId,departmentId,loggedInUserId,ROLE.valueOf(loggedInUserRole));
    }

    @GetMapping("/role/{role}")
    public Flux<UserResponse> getUsersByRole(@PathVariable String role,@RequestHeader("X-USER-ID") String loggedInUserId,@RequestHeader("X-USER-ROLE") String loggedInUserRole) {
    	return userService.getUsersByRole(role,ROLE.valueOf(loggedInUserRole));
    }

    // INTERNAL API
    // Used by grievance-service during escalation
    @GetMapping("/supervisor/department/{departmentId}")
    public Mono<String> getSupervisorByDepartment(
            @PathVariable String departmentId) {

        return userRepository
                .findByRoleAndDepartmentId(ROLE.SUPERVISOR, departmentId)
                .map(User::getId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Supervisor not found for department " + departmentId)));
    }
}
