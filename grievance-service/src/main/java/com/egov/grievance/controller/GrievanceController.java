package com.egov.grievance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.egov.grievance.dto.CreateGrievanceRequest;
import com.egov.grievance.service.GrievanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
