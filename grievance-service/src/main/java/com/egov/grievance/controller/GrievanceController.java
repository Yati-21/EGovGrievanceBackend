package com.egov.grievance.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.egov.grievance.dto.AssignGrievanceRequest;
import com.egov.grievance.dto.CreateGrievanceRequest;
import com.egov.grievance.model.Grievance;
import com.egov.grievance.service.GrievanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/grievances")
@RequiredArgsConstructor
public class GrievanceController {

    private final GrievanceService grievanceService;

    @PostMapping
    public Mono<ResponseEntity<String>> createGrievance( @RequestHeader("X-USER-ID") String userId,@RequestHeader("X-USER-ROLE") String role, @Valid @RequestBody CreateGrievanceRequest request) 
    {
        return grievanceService.createGrievance(userId, role, request)
                .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
    }
    
    @PutMapping("/{grievanceId}/assign")
    public Mono<ResponseEntity<Map<String, String>>> assignGrievance(
            @PathVariable String grievanceId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role,
            @Valid @RequestBody AssignGrievanceRequest request) {

        return grievanceService
                .assignGrievance(
                        grievanceId,userId,role,request.getOfficerId())
                .thenReturn(ResponseEntity.ok(
                        Map.of("message", "Grievance assigned successfully")));
    }
    
    @PutMapping("/{grievanceId}/in-review")
    public Mono<ResponseEntity<Map<String, String>>> markInReview(
            @PathVariable String grievanceId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {

        return grievanceService
                .markInReview(grievanceId, userId, role)
                .thenReturn(ResponseEntity.ok(
                        Map.of("message", "Grievance moved to IN_REVIEW")));
    }
    @PutMapping("/{grievanceId}/resolve")
    public Mono<ResponseEntity<Map<String, String>>> resolveGrievance(
            @PathVariable String grievanceId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {

        return grievanceService
                .resolveGrievance(grievanceId, userId, role)
                .thenReturn(ResponseEntity.ok(
                        Map.of("message", "Grievance resolved")));
    }
    
    @PutMapping("/{grievanceId}/close")
    public Mono<ResponseEntity<Map<String, String>>> closeGrievance(
            @PathVariable String grievanceId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {

        return grievanceService
                .closeGrievance(grievanceId, userId, role)
                .thenReturn(ResponseEntity.ok(
                        Map.of("message", "Grievance closed")));
    }
    
    @PutMapping("/{grievanceId}/reopen")
    public Mono<ResponseEntity<Map<String, String>>> reopen(
            @PathVariable String grievanceId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {

        return grievanceService.reopenGrievance(grievanceId, userId, role)
                .thenReturn(ResponseEntity.ok(
                        Map.of("message", "Grievance reopened")));
    }

    
    @GetMapping("/assigned")
    public Flux<Grievance> getAssignedGrievances(
            @RequestHeader("X-USER-ID") String officerId,
            @RequestHeader("X-USER-ROLE") String role) {

        return grievanceService.getAssignedGrievances(officerId, role);
    }

}
