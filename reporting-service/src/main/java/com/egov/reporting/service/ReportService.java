package com.egov.reporting.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.egov.reporting.client.GrievanceClient;
import com.egov.reporting.client.UserClient;
import com.egov.reporting.dto.GrievanceDTO;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final GrievanceClient grievanceClient;
    private final UserClient userClient;

    public Flux<GrievanceDTO> getGrievancesByStatus(String userId, String role, String status) {
        return grievanceClient.getGrievances(userId, role, status, null);
    }

    public Flux<GrievanceDTO> getGrievancesByDepartment(String userId, String role, String departmentId) {
        return grievanceClient.getGrievances(userId, role, null, departmentId);
    }

    public Mono<Double> getAverageResolutionTime(String userId, String role, String departmentId) {
        return grievanceClient.getGrievances(userId, role, "RESOLVED", departmentId)
                .filter(g -> g.getResolvedAt() != null)
                .collect(Collectors.averagingDouble(g -> 
                    Duration.between(g.getCreatedAt(), g.getResolvedAt()).toHours()
                ));
    }

    public Mono<Map<String, Long>> getDepartmentPerformance(String userId, String role) {
        return grievanceClient.getGrievances(userId, role, null, null)
                .collect(Collectors.groupingBy(
                        GrievanceDTO::getDepartmentId,
                        Collectors.counting()
                ));
    }

    public Mono<Map<String, Long>> getUserGrievanceSummary(String targetUserId, String userId, String role) {
        return userClient.validateUserExists(targetUserId)
               .flatMap(exists -> {
                   if (!exists) return Mono.error(new IllegalArgumentException("User not found"));
                   
                   return grievanceClient.getGrievances(userId, role, null, null)
                       .filter(g -> targetUserId.equals(g.getCitizenId()))
                       .collect(Collectors.groupingBy(
                               GrievanceDTO::getStatus,
                               Collectors.counting()
                       ));
               });
   }

   public Flux<GrievanceDTO> getSlaBreaches(String userId, String role) {
       return grievanceClient.getSlaBreaches(userId, role);
   }
}