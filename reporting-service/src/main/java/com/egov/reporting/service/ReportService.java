package com.egov.reporting.service;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.egov.reporting.client.GrievanceClient;
import com.egov.reporting.client.UserClient;
import com.egov.reporting.dto.GrievanceDTO;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String NO_DATA = "No data available";

    private final GrievanceClient grievanceClient;
    private final UserClient userClient;

    public Flux<GrievanceDTO> getGrievances(String userId, String role, String status, String departmentId) {
        return grievanceClient.getGrievances(userId, role, status, departmentId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No grievances found")));
    }

    public Flux<GrievanceDTO> getSlaBreaches(String userId, String role) {
        return grievanceClient.getSlaBreaches(userId, role)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No SLA breaches found")));
    }

    public Mono<Map<String, Object>> getAverageResolutionTime(String userId, String role, String departmentId) {
        // include both RESOLVED and CLOSED
        // then filter by resolvedAt
        return grievanceClient.getGrievances(userId, role, null, departmentId)
                .filter(g -> g.getResolvedAt() != null)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, NO_DATA)))
                .collect(Collectors
                        .averagingDouble(g -> Duration.between(g.getCreatedAt(), g.getResolvedAt()).toMinutes()))
                .flatMap(avg -> {
                    if (avg.isNaN()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, NO_DATA));
                    }
                    return Mono.just(Map.of(
                            "averageTime", avg,
                            "unit", "minutes"));
                });
    }

    public Mono<Map<String, Integer>> getDepartmentPerformance(String userId, String role) {
        return grievanceClient.getGrievances(userId, role, null, null)
                .collect(Collectors.groupingBy(
                        GrievanceDTO::getDepartmentId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)))
                .flatMap(map -> {
                    if (map.isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, NO_DATA));
                    }
                    return Mono.just(map);
                });
    }

    public Mono<Map<String, Integer>> getUserGrievanceSummary(String targetUserId, String userId, String role) {
        return userClient.validateUserExists(targetUserId, userId, role)
                .flatMap(exists -> {
                    if (!exists)
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    return grievanceClient.getGrievances(userId, role, null, null)
                            .filter(g -> targetUserId.equals(userId) || targetUserId.equals(g.getCitizenId())
                                    || targetUserId.equals(g.getAssignedOfficerId()))
                            .collect(Collectors.groupingBy(
                                    GrievanceDTO::getStatus,
                                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)))
                            .map(map -> {
                                if (map.isEmpty()) {
                                    return Map.<String, Integer>of();
                                }
                                return map;
                            });
                });
    }

    public Mono<Map<String, Object>> getPublicStats() {
        return grievanceClient.getGrievances("SYSTEM", "ADMIN", null, null)
                .collectList()
                .map(grievances -> {
                    long total = grievances.size();
                    if (total == 0) {
                        return Map.of(
                                "resolvedCount", 0,
                                "resolutionRate", 0.0,
                                "avgResolutionTime", 0.0);
                    }

                    long resolvedCount = grievances.stream()
                            .filter(g -> "RESOLVED".equals(g.getStatus()) || "CLOSED".equals(g.getStatus()))
                            .count();

                    double resolutionRate = ((double) resolvedCount / total) * 100;

                    //average resolution time - resolved/closed 
                    double avgTimeHours = grievances.stream()
                            .filter(g -> ("RESOLVED".equals(g.getStatus()) || "CLOSED".equals(g.getStatus()))
                                    && g.getResolvedAt() != null && g.getCreatedAt() != null)
                            .mapToLong(g -> Duration.between(g.getCreatedAt(), g.getResolvedAt()).toMinutes())
                            .average()
                            .orElse(0.0) / 60.0;

                    return Map.<String, Object>of(
                            "resolvedCount", resolvedCount,
                            "resolutionRate", Math.round(resolutionRate * 10.0) / 10.0, // round to 1 decimal
                            "avgResolutionTime", Math.round(avgTimeHours * 10.0) / 10.0);
                });
    }
}