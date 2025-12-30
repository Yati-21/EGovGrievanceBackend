package com.egov.grievance.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.egov.grievance.dto.CreateGrievanceRequest;
import com.egov.grievance.model.GRIEVANCE_STATUS;
import com.egov.grievance.model.Grievance;
import com.egov.grievance.repository.GrievanceRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GrievanceService {

        private final GrievanceRepository grievanceRepository;
        private final GrievanceHistoryService grievanceHistoryService;
        private final ReferenceDataService referenceDataService;

        public Mono<String> createGrievance(String userId, String role, CreateGrievanceRequest request) {
                if (!"CITIZEN".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException("Only CITIZEN can create grievances"));
                }

                return referenceDataService
                                .validateDepartmentAndCategory(
                                                request.getDepartmentId(),
                                                request.getCategoryId())
                                .then(Mono.defer(() -> {

                                        Grievance grievance = Grievance.builder()
                                                        .citizenId(userId)
                                                        .departmentId(request.getDepartmentId())
                                                        .categoryId(request.getCategoryId())
                                                        .title(request.getTitle())
                                                        .description(request.getDescription())
                                                        .status(GRIEVANCE_STATUS.SUBMITTED)
                                                        .isEscalated(false)
                                                        .createdAt(Instant.now())
                                                        .updatedAt(Instant.now())
                                                        .build();

                                        return grievanceRepository.save(grievance);
                                }))
                                // add Submitted status in status history table too
                                .flatMap(saved -> grievanceHistoryService.createInitialHistory(
                                                saved.getId(),
                                                userId))
                                .map(ignored -> ignored);
        }

        public Mono<Void> assignGrievance(
                        String grievanceId,
                        String assignedBy,
                        String role,
                        String officerId) {

                if (!"ADMIN".equalsIgnoreCase(role)
                                && !"SUPERVISOR".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException(
                                        "Only ADMIN or SUPERVISOR can assign grievances"));
                }

                return grievanceRepository.findById(grievanceId)
                                .switchIfEmpty(Mono.error(
                                                new IllegalArgumentException("Grievance not found")))
                                .flatMap(grievance -> {

                                        if (grievance.getStatus() != GRIEVANCE_STATUS.SUBMITTED
                                                        && grievance.getStatus() != GRIEVANCE_STATUS.REOPENED) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Grievance cannot be assigned in current status"));
                                        }

                                        GRIEVANCE_STATUS oldStatus = grievance.getStatus();

                                        grievance.setAssignedOfficerId(officerId);
                                        grievance.setStatus(GRIEVANCE_STATUS.ASSIGNED);
                                        grievance.setUpdatedAt(Instant.now());

                                        return grievanceRepository.save(grievance)
                                                        .then(grievanceHistoryService.addHistory(
                                                                        grievanceId,
                                                                        oldStatus,
                                                                        GRIEVANCE_STATUS.ASSIGNED,
                                                                        assignedBy));
                                });
        }
}
