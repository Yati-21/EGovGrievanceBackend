package com.egov.grievance.service;

import java.time.Duration;
import java.time.Instant;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.egov.grievance.dto.CreateGrievanceRequest;
import com.egov.grievance.dto.UserResponse;
import com.egov.grievance.event.GrievanceStatusChangedEvent;
import com.egov.grievance.exception.UserNotFoundException;
import com.egov.grievance.model.GRIEVANCE_STATUS;
import com.egov.grievance.model.Grievance;
import com.egov.grievance.model.GrievanceDocument;
import com.egov.grievance.repository.GrievanceDocumentRepository;
import com.egov.grievance.repository.GrievanceRepository;

import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GrievanceService {

        private final GrievanceRepository grievanceRepository;
        private final GrievanceDocumentRepository grievanceDocumentRepository;
        private final GrievanceHistoryService grievanceHistoryService;
        private final ReferenceDataService referenceDataService;
        private final WebClient.Builder webClientBuilder;
        private final GrievanceEventPublisher grievanceEventPublisher;


        public Mono<String> createGrievance(String userId, String role, CreateGrievanceRequest request,Flux<FilePart> files) {
                if (!"CITIZEN".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException("Only CITIZEN can create grievances"));
                }
                return referenceDataService
                                .validateDepartmentAndCategory(request.getDepartmentId(),request.getCategoryId())
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
                                .flatMap(savedGrievance -> {
                                        Flux<FilePart> fileFlux = files != null ? files : Flux.empty();
                                        return fileFlux
                                                .flatMap(file -> saveFile(file, savedGrievance.getId(), userId))
                                                .then()
                                                .then(grievanceHistoryService.createInitialHistory(
                                                                savedGrievance.getId(),
                                                                userId))
                                                .then(Mono.fromRunnable(() -> {
                                                    grievanceEventPublisher.publishStatusChange(
                                                        new GrievanceStatusChangedEvent(
                                                            savedGrievance.getId(),
                                                            savedGrievance.getCitizenId(),
                                                            savedGrievance.getDepartmentId(),
                                                            null, //no officer yet
                                                            null, //no  old status yet
                                                            GRIEVANCE_STATUS.SUBMITTED.name(),
                                                            userId,
                                                            Instant.now()
                                                        )
                                                    );
                                                }))
                                                .thenReturn(savedGrievance.getId());
                                });
        }


        public Mono<Void> assignGrievance(String grievanceId,String assignedBy,String role,String officerId) 
        {

                if (!"ADMIN".equalsIgnoreCase(role)&& !"SUPERVISOR".equalsIgnoreCase(role)) 
                {
                    return Mono.error(new IllegalArgumentException("Only ADMIN or SUPERVISOR can assign grievances"));
                }

                return grievanceRepository.findById(grievanceId)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Grievance not found")))
                    .flatMap(grievance -> {
                            if (grievance.getStatus() != GRIEVANCE_STATUS.SUBMITTED
                                            && grievance.getStatus() != GRIEVANCE_STATUS.REOPENED
                                            && grievance.getStatus() != GRIEVANCE_STATUS.ESCALATED) {
                                    return Mono.error(new IllegalArgumentException("Grievance cannot be assigned in current status"));
                            }
                            // if assigned by SUPERVISOR -validate their department
                            Mono<Void> supervisorValidation;
                            if ("SUPERVISOR".equalsIgnoreCase(role)) 
                            {
                                    supervisorValidation = fetchUserById(assignedBy,"Assigned By user not found")
                                                    .flatMap(supervisor -> {
                                                            if (!grievance.getDepartmentId().equals(supervisor.getDepartmentId())) {
                                                                    return Mono.error(new IllegalArgumentException("Supervisor can only assign grievances for their own department"));
                                                            }
                                                            return Mono.empty();
                                                    });
                            } 
                            else {
                                    supervisorValidation = Mono.empty();
                            }

                            // Validate Officer
                            Mono<Void> officerValidation = fetchUserById(officerId,
                                    "Officer not found with id: " + officerId)
                                    .flatMap(officer -> {
                                            if (!"OFFICER".equalsIgnoreCase(officer.getRole())) {
                                                    return Mono.error(new IllegalArgumentException(
                                                                    "Assigned user is not an OFFICER"));
                                            }
                                            if (!grievance.getDepartmentId()
                                                            .equals(officer.getDepartmentId())) {
                                                    return Mono.error(new IllegalArgumentException(
                                                                    "Officer does not belong to the grievance department"));
                                            }
                                            return Mono.empty();
                                    });
                            return supervisorValidation
                                    .then(officerValidation)
                                    .then(Mono.defer(() -> {
                                            GRIEVANCE_STATUS oldStatus = grievance.getStatus();
                                            grievance.setAssignedOfficerId(officerId);
                                            grievance.setStatus(GRIEVANCE_STATUS.ASSIGNED);
                                            grievance.setUpdatedAt(Instant.now());

                                            return grievanceRepository.save(grievance)
                                                .then(grievanceHistoryService
                                                        .addHistory(grievanceId,
                                                                    oldStatus,
                                                                    GRIEVANCE_STATUS.ASSIGNED,
                                                                    assignedBy))
                                                .then(Mono.fromRunnable(() -> 
                                                {
                                                    grievanceEventPublisher.publishStatusChange
                                                    (
                                                        new GrievanceStatusChangedEvent
                                                        (
                                                            grievanceId,
                                                            grievance.getCitizenId(),
                                                            grievance.getDepartmentId(),
                                                            officerId,
                                                            oldStatus.name(),
                                                            GRIEVANCE_STATUS.ASSIGNED.name(),
                                                            assignedBy,
                                                            Instant.now()
                                                        )
                                                    );
                                                }));
                                    }));
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
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("Grievance not found")))
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
                                                                        officerId))
                                                        .then(Mono.fromRunnable(() -> {
                                                            grievanceEventPublisher.publishStatusChange(
                                                                new GrievanceStatusChangedEvent(
                                                                    grievanceId,
                                                                    grievance.getCitizenId(),
                                                                    grievance.getDepartmentId(),
                                                                    grievance.getAssignedOfficerId(),
                                                                    oldStatus.name(),
                                                                    GRIEVANCE_STATUS.IN_REVIEW.name(),
                                                                    officerId,
                                                                    Instant.now()
                                                                )
                                                            );
                                                        }));
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
                                                                        officerId))
                                                        .then(Mono.fromRunnable(() -> {
                                                            grievanceEventPublisher.publishStatusChange(
                                                                new GrievanceStatusChangedEvent(
                                                                    grievanceId,
                                                                    grievance.getCitizenId(),
                                                                    grievance.getDepartmentId(),
                                                                    grievance.getAssignedOfficerId(),
                                                                    oldStatus.name(),
                                                                    GRIEVANCE_STATUS.RESOLVED.name(),
                                                                    officerId,
                                                                    Instant.now()
                                                                )
                                                            );
                                                        }));
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
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Grievance not found")))
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
                                                                userId))
                                                .then(Mono.fromRunnable(() -> {
                                                    grievanceEventPublisher.publishStatusChange(
                                                        new GrievanceStatusChangedEvent(
                                                            grievanceId,
                                                            grievance.getCitizenId(),
                                                            grievance.getDepartmentId(),
                                                            grievance.getAssignedOfficerId(),
                                                            oldStatus.name(),
                                                            GRIEVANCE_STATUS.CLOSED.name(),
                                                            userId,
                                                            Instant.now()
                                                        )
                                                    );
                                                }));
                        });
        }

        public Mono<Void> reopenGrievance(
                        String grievanceId,
                        String citizenId,
                        String role) {

                if (!"CITIZEN".equalsIgnoreCase(role)) {
                        return Mono.error(new IllegalArgumentException("Only CITIZEN can reopen grievance"));
                }

                return grievanceRepository.findById(grievanceId)
	                    .switchIfEmpty(Mono.error(
	                                    new IllegalArgumentException("Grievance not found")))
	                    .flatMap(grievance -> {
	
	                            if (!citizenId.equals(grievance.getCitizenId())) {
	                                    return Mono.error(new IllegalArgumentException("Cannot reopen someone else's grievance"));
	                            }
	
	                            if (grievance.getStatus() != GRIEVANCE_STATUS.CLOSED) {
	                                    return Mono.error(new IllegalArgumentException("Only CLOSED grievance can be reopened"));
	                            }
	
	                            if (grievance.getResolvedAt() == null) {
	                                    return Mono.error(new IllegalArgumentException("Resolved timestamp missing"));
	                            }
	
	                            long daysSinceResolved = java.time.Duration
	                                            .between(grievance.getResolvedAt(), Instant.now())
	                                            .toDays();
	
	                            if (daysSinceResolved > 7) 
	                            {
	                                    return Mono.error(new IllegalArgumentException("Reopen window expired (7 days)"));
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
	                                                            citizenId))
	                                            .then(Mono.fromRunnable(() -> {
	                                                grievanceEventPublisher.publishStatusChange(
	                                                    new GrievanceStatusChangedEvent(
	                                                        grievanceId,
	                                                        grievance.getCitizenId(),
	                                                        grievance.getDepartmentId(),
	                                                        null,  // no officer after reopen
	                                                        oldStatus.name(),
	                                                        GRIEVANCE_STATUS.REOPENED.name(),
	                                                        citizenId,
	                                                        Instant.now()
	                                                    )
	                                                );
	                                            }));
	                    });
        }

        public Flux<Grievance> getAssignedGrievances(String officerId,String role) 
        {
                if (!"OFFICER".equalsIgnoreCase(role)) 
                {
                    return Flux.error(new IllegalArgumentException("Only OFFICER can view assigned grievances"));
                }
                return grievanceRepository.findByAssignedOfficerId(officerId);
        }

        public Flux<Grievance> getGrievancesByCitizen(String citizenId) 
        {
                return grievanceRepository.findByCitizenId(citizenId);
        }

        public Mono<Void> escalateGrievance(String grievanceId,String citizenId,String role) 
        {
            if (!"CITIZEN".equalsIgnoreCase(role)) {
                    return Mono.error(new IllegalArgumentException("Only CITIZEN can escalate grievance"));
            }

            return grievanceRepository.findById(grievanceId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Grievance not found")))
                .flatMap(grievance -> 
                {
                    if (!citizenId.equals(grievance.getCitizenId())) {
                            return Mono.error(new IllegalArgumentException("Cannot escalate someone else's grievance"));
                    }
                    if (grievance.getStatus() == GRIEVANCE_STATUS.RESOLVED
                                    || grievance.getStatus() == GRIEVANCE_STATUS.CLOSED) {
                            return Mono.error(new IllegalArgumentException("Cannot escalate resolved or closed grievance"));
                    }
                    if (Boolean.TRUE.equals(grievance.getIsEscalated())) {
                            return Mono.error(new IllegalArgumentException("Grievance already escalated"));
                    }
                    //SLA check
                    return referenceDataService
                        .getSlaHours(grievance.getDepartmentId(),
                                     grievance.getCategoryId())
                        .flatMap(slaHours -> 
                        {
                                long hoursElapsed = Duration
                                                .between(grievance.getCreatedAt(),Instant.now())
                                                .toHours();

                                if (hoursElapsed <= slaHours) 
                                {
                                        return Mono.error(new IllegalArgumentException("SLA not breached yet"));
                                }

                                GRIEVANCE_STATUS oldStatus = grievance.getStatus();

                                return webClientBuilder.build()
                                    .get()
                                    .uri("http://user-service/users/supervisor/department/{departmentId}",
                                                    grievance.getDepartmentId())
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .flatMap(supervisorId -> 
                                    {
                                            grievance.setStatus(GRIEVANCE_STATUS.ESCALATED);
                                            grievance.setIsEscalated(true);
                                            grievance.setAssignedOfficerId(supervisorId);
                                            grievance.setUpdatedAt(Instant.now());

                                            return grievanceRepository
                                                            .save(grievance)
                                                            .then(grievanceHistoryService
                                                                            .addHistory(grievanceId,
                                                                                            oldStatus,
                                                                                            GRIEVANCE_STATUS.ESCALATED,
                                                                                            citizenId))
                                                            .then(Mono.fromRunnable(() -> {
                                                                grievanceEventPublisher.publishStatusChange(
                                                                    new GrievanceStatusChangedEvent(
                                                                        grievanceId,
                                                                        grievance.getCitizenId(),
                                                                        grievance.getDepartmentId(),
                                                                        supervisorId,
                                                                        oldStatus.name(),
                                                                        GRIEVANCE_STATUS.ESCALATED.name(),
                                                                        citizenId,
                                                                        Instant.now()
                                                                    )
                                                                );
                                                            }));
                                    });
                        });
                });
        }

        private Mono<UserResponse> fetchUserById(String userId, String errorMessage) {
                return webClientBuilder.build()
                        .get()
                        .uri("http://user-service/users/{id}", userId)
                        .retrieve()
                        .onStatus(org.springframework.http.HttpStatusCode::is4xxClientError,
                                        response -> Mono.error(new IllegalArgumentException(errorMessage)))
                        .bodyToMono(UserResponse.class);
        }
        public Flux<GrievanceDocument> getGrievanceDocuments(String grievanceId) {
            return grievanceRepository.existsById(grievanceId)
                    .flatMapMany(exists -> {
                        if (!exists) {
                            return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Grievance not found"));
                        }
                        return grievanceDocumentRepository.findByGrievanceId(grievanceId);
                    });
        }

        public Mono<Resource> downloadDocument(String grievanceId, String documentId) {
            return grievanceRepository.existsById(grievanceId)
                    .flatMap(exists -> {
                        if (!exists) {
                            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Grievance not found"));
                        }
                        return grievanceDocumentRepository.findById(documentId)
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")))
                                .flatMap(doc -> {
                                    if (!doc.getGrievanceId().equals(grievanceId)) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to this grievance"));
                                    }
                                    Path path = Paths.get(doc.getFilePath());
                                    Resource resource = new FileSystemResource(path);
                                    if (resource.exists() && resource.isReadable()) {
                                        return Mono.just(resource);
                                    } else {
                                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on server"));
                                    }
                                });
                    });
        }
        private Mono<Void> saveFile(FilePart filePart, String grievanceId, String userId) {
            return Mono.fromRunnable(() -> {
                    String uploadDir = "C:/Users/YATI/Desktop/EGovGrievance/uploads/" + grievanceId;
                    File dir = new java.io.File(uploadDir);
                    if (!dir.exists()) {
                            dir.mkdirs();
                    }
            }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                            .then(Mono.defer(() -> {
                                    String uploadDir = "C:/Users/YATI/Desktop/EGovGrievance/uploads/" + grievanceId;
                                    String fileName = filePart.filename();
                                    Path filePath = Paths.get(uploadDir, fileName);

                                    GrievanceDocument doc = GrievanceDocument.builder()
                                            .grievanceId(grievanceId)
                                            .uploadedBy(userId)
                                            .fileName(fileName)
                                            .fileType(filePart.headers().getContentType() != null
                                                            ? filePart.headers().getContentType().toString()
                                                            : "application/octet-stream")
                                            .filePath(filePath.toString())
                                            .uploadedAt(Instant.now())
                                            .build();

                                    return filePart.transferTo(filePath)
                                                    .then(grievanceDocumentRepository.save(doc))
                                                    .then();
                            }));
    }
}
