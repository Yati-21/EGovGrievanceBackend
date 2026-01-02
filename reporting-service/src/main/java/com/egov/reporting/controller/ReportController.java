package com.egov.reporting.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.egov.reporting.dto.GrievanceDTO;
import com.egov.reporting.service.ReportService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/grievances/status/{status}")
    public Flux<GrievanceDTO> getGrievancesByStatus(
            @PathVariable String status,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return reportService.getGrievancesByStatus(userId, role, status);
    }

    @GetMapping("/grievances/department/{departmentId}")
    public Flux<GrievanceDTO> getGrievancesByDepartment(
            @PathVariable String departmentId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return reportService.getGrievancesByDepartment(userId, role, departmentId);
    }

    @GetMapping("/avg-resolution-time")
    public Mono<ResponseEntity<Double>> getAvgResolutionTime(
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return reportService.getAverageResolutionTime(userId, role, null)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/avg-resolution-time/department/{departmentId}")
    public Mono<ResponseEntity<Double>> getDeptAvgResolutionTime(
            @PathVariable String departmentId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return reportService.getAverageResolutionTime(userId, role, departmentId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/department-performance")
    public Mono<ResponseEntity<Map<String, Long>>> getDepartmentPerformance(
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return reportService.getDepartmentPerformance(userId, role)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/user/{targetUserId}")
    public Mono<ResponseEntity<Map<String, Long>>> getUserSummary(
            @PathVariable String targetUserId,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return reportService.getUserGrievanceSummary(targetUserId, userId, role)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/grievances/sla-breaches")
    public Flux<GrievanceDTO> getSlaBreaches(
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-ROLE") String role) {
        return reportService.getSlaBreaches(userId, role);
    }
}