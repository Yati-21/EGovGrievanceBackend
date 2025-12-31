package com.egov.grievance.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.egov.grievance.dto.CreateGrievanceRequest;
import com.egov.grievance.model.GRIEVANCE_STATUS;
import com.egov.grievance.model.Grievance;
import com.egov.grievance.repository.GrievanceRepository;

import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GrievanceService {

        private final GrievanceRepository grievanceRepository;
        private final GrievanceHistoryService grievanceHistoryService;
        private final ReferenceDataService referenceDataService;
        private final WebClient.Builder webClientBuilder;

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
                                                        && grievance.getStatus() != GRIEVANCE_STATUS.REOPENED
                                                        && grievance.getStatus() != GRIEVANCE_STATUS.ESCALATED) {
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

        public Mono<Void> markInReview(
                        String grievanceId,
                        String officerId,
                        String role) {

                if (!"OFFICER".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException(
                                        "Only OFFICER can start review"));
                }

                return grievanceRepository.findById(grievanceId)
                                .switchIfEmpty(Mono.error(
                                                new IllegalArgumentException("Grievance not found")))
                                .flatMap(grievance -> {

                                        if (!officerId.equals(grievance.getAssignedOfficerId())) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Officer not assigned to this grievance"));
                                        }

                                        if (grievance.getStatus() != GRIEVANCE_STATUS.ASSIGNED) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Grievance is not in ASSIGNED state"));
                                        }

                                        GRIEVANCE_STATUS oldStatus = grievance.getStatus();

                                        grievance.setStatus(GRIEVANCE_STATUS.IN_REVIEW);
                                        grievance.setUpdatedAt(Instant.now());

                                        return grievanceRepository.save(grievance)
                                                        .then(grievanceHistoryService.addHistory(
                                                                        grievanceId,
                                                                        oldStatus,
                                                                        GRIEVANCE_STATUS.IN_REVIEW,
                                                                        officerId));
                                });
        }

        public Mono<Void> resolveGrievance(
                        String grievanceId,
                        String officerId,
                        String role) {

                if (!"OFFICER".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException(
                                        "Only OFFICER can resolve grievances"));
                }

                return grievanceRepository.findById(grievanceId)
                                .switchIfEmpty(Mono.error(
                                                new IllegalArgumentException("Grievance not found")))
                                .flatMap(grievance -> {

                                        if (!officerId.equals(grievance.getAssignedOfficerId())) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Officer not assigned to this grievance"));
                                        }

                                        if (grievance.getStatus() != GRIEVANCE_STATUS.IN_REVIEW) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Grievance not in IN_REVIEW state"));
                                        }

                                        GRIEVANCE_STATUS oldStatus = grievance.getStatus();

                                        grievance.setStatus(GRIEVANCE_STATUS.RESOLVED);
                                        grievance.setResolvedAt(Instant.now());
                                        grievance.setUpdatedAt(Instant.now());

                                        return grievanceRepository.save(grievance)
                                                        .then(grievanceHistoryService.addHistory(
                                                                        grievanceId,
                                                                        oldStatus,
                                                                        GRIEVANCE_STATUS.RESOLVED,
                                                                        officerId));
                                });
        }

        public Mono<Void> closeGrievance(
                        String grievanceId,
                        String userId,
                        String role) {

                if (!"ADMIN".equalsIgnoreCase(role)
                                && !"SUPERVISOR".equalsIgnoreCase(role)
                                && !"OFFICER".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException(
                                        "Only ADMIN, SUPERVISOR or OFFICER can close grievance"));
                }

                return grievanceRepository.findById(grievanceId)
                                .switchIfEmpty(Mono.error(
                                                new IllegalArgumentException("Grievance not found")))
                                .flatMap(grievance -> {

                                        if (grievance.getStatus() != GRIEVANCE_STATUS.RESOLVED) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Only RESOLVED grievance can be closed"));
                                        }

                                        GRIEVANCE_STATUS oldStatus = grievance.getStatus();

                                        grievance.setStatus(GRIEVANCE_STATUS.CLOSED);
                                        grievance.setUpdatedAt(Instant.now());

                                        return grievanceRepository.save(grievance)
                                                        .then(grievanceHistoryService.addHistory(
                                                                        grievanceId,
                                                                        oldStatus,
                                                                        GRIEVANCE_STATUS.CLOSED,
                                                                        userId));
                                });
        }

        public Mono<Void> reopenGrievance(
                        String grievanceId,
                        String citizenId,
                        String role) {

                if (!"CITIZEN".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException(
                                        "Only CITIZEN can reopen grievance"));
                }

                return grievanceRepository.findById(grievanceId)
                                .switchIfEmpty(Mono.error(
                                                new IllegalArgumentException("Grievance not found")))
                                .flatMap(grievance -> {

                                        if (!citizenId.equals(grievance.getCitizenId())) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Cannot reopen someone else's grievance"));
                                        }

                                        if (grievance.getStatus() != GRIEVANCE_STATUS.CLOSED) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Only CLOSED grievance can be reopened"));
                                        }

                                        if (grievance.getResolvedAt() == null) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Resolved timestamp missing"));
                                        }

                                        long daysSinceResolved = java.time.Duration
                                                        .between(grievance.getResolvedAt(), Instant.now())
                                                        .toDays();

                                        if (daysSinceResolved > 7) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Reopen window expired (7 days)"));
                                        }

                                        GRIEVANCE_STATUS oldStatus = grievance.getStatus();

                                        grievance.setStatus(GRIEVANCE_STATUS.REOPENED);
                                        grievance.setUpdatedAt(Instant.now());
                                        grievance.setAssignedOfficerId(null);
                                        grievance.setIsEscalated(false);

                                        return grievanceRepository.save(grievance)
                                                        .then(grievanceHistoryService.addHistory(
                                                                        grievanceId,
                                                                        oldStatus,
                                                                        GRIEVANCE_STATUS.REOPENED,
                                                                        citizenId));
                                });
        }

        public Flux<Grievance> getAssignedGrievances(
                        String officerId,
                        String role) {

                if (!"OFFICER".equalsIgnoreCase(role)) {
                        return Flux.error(new IllegalArgumentException(
                                        "Only OFFICER can view assigned grievances"));
                }

                return grievanceRepository.findByAssignedOfficerId(officerId);
        }

        public Flux<Grievance> getGrievancesByCitizen(String citizenId) {
                return grievanceRepository.findByCitizenId(citizenId);
        }

        public Mono<Void> escalateGrievance(
                        String grievanceId,
                        String citizenId,
                        String role) {

                if (!"CITIZEN".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException(
                                        "Only CITIZEN can escalate grievance"));
                }

                return grievanceRepository.findById(grievanceId)
                                .switchIfEmpty(Mono.error(
                                                new IllegalArgumentException("Grievance not found")))
                                .flatMap(grievance -> {

                                        if (!citizenId.equals(grievance.getCitizenId())) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Cannot escalate someone else's grievance"));
                                        }

                                        if (grievance.getStatus() == GRIEVANCE_STATUS.RESOLVED
                                                        || grievance.getStatus() == GRIEVANCE_STATUS.CLOSED) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Cannot escalate resolved or closed grievance"));
                                        }

                                        if (Boolean.TRUE.equals(grievance.getIsEscalated())) {
                                                return Mono.error(new IllegalArgumentException(
                                                                "Grievance already escalated"));
                                        }

                                        // SLA check
                                        return referenceDataService
                                                        .getSlaHours(
                                                                        grievance.getDepartmentId(),
                                                                        grievance.getCategoryId())
                                                        .flatMap(slaHours -> {

                                                                long hoursElapsed = java.time.Duration
                                                                                .between(grievance.getCreatedAt(),
                                                                                                Instant.now())
                                                                                .toHours();

                                                                if (hoursElapsed <= slaHours) {
                                                                        return Mono.error(new IllegalArgumentException(
                                                                                        "SLA not breached yet"));
                                                                }

                                                                GRIEVANCE_STATUS oldStatus = grievance.getStatus();

                                                                return webClientBuilder.build()
                                                                                .get()
                                                                                .uri("http://user-service/users/supervisor/department/{departmentId}",
                                                                                                grievance.getDepartmentId())
                                                                                .retrieve()
                                                                                .bodyToMono(String.class)
                                                                                .flatMap(supervisorId -> {

                                                                                        grievance.setStatus(
                                                                                                        GRIEVANCE_STATUS.ESCALATED);
                                                                                        grievance.setIsEscalated(true);
                                                                                        grievance.setAssignedOfficerId(
                                                                                                        supervisorId);
                                                                                        grievance.setUpdatedAt(
                                                                                                        Instant.now());

                                                                                        return grievanceRepository
                                                                                                        .save(grievance)
                                                                                                        .then(grievanceHistoryService
                                                                                                                        .addHistory(
                                                                                                                                        grievanceId,
                                                                                                                                        oldStatus,
                                                                                                                                        GRIEVANCE_STATUS.ESCALATED,
                                                                                                                                        citizenId));
                                                                                });
                                                        });
                                });
        }

}
