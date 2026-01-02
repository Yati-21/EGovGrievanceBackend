package com.egov.grievance.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.egov.grievance.dto.AssignGrievanceRequest;
import com.egov.grievance.dto.CreateGrievanceRequest;
import com.egov.grievance.model.Grievance;
import com.egov.grievance.repository.GrievanceHistoryRepository;
import com.egov.grievance.repository.GrievanceRepository;
import com.egov.grievance.service.GrievanceService;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.egov.grievance.model.GrievanceDocument;
import com.egov.grievance.model.GrievanceStatusHistory;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/grievances")
@RequiredArgsConstructor
public class GrievanceController {

        private final GrievanceService grievanceService;

        @Autowired
        private GrievanceHistoryRepository grievanceHistoryRepository;

        @Autowired
        private GrievanceRepository grievanceRepository;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public Mono<ResponseEntity<String>> createGrievance(
                        @RequestHeader("X-USER-ID") String userId,
                        @RequestHeader("X-USER-ROLE") String role,
                        @RequestPart("request") @Valid CreateGrievanceRequest request,
                        @RequestPart(name = "files", required = false) Flux<FilePart> files) {

                return grievanceService.createGrievance(userId, role, request, files)
                                .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
        }
        @GetMapping
        public Flux<Grievance> getAllGrievances(
		                @RequestHeader("X-USER-ID") String userId,
		                @RequestHeader("X-USER-ROLE") String role,
		                @RequestParam(required = false) String status,
		                @RequestParam(required = false) String departmentId) {
		
		            return grievanceService.getGrievances(status, departmentId, role, userId);
        }

        @GetMapping("/sla-breaches")
        public Flux<Grievance> getSlaBreaches(
                @RequestHeader("X-USER-ID") String userId,
                @RequestHeader("X-USER-ROLE") String role) {
                return grievanceService.getSlaBreaches(userId, role);
        }

        @PutMapping("/{grievanceId}/assign")
        public Mono<Void> assignGrievance(
                        @PathVariable String grievanceId,
                        @RequestHeader("X-USER-ID") String userId,
                        @RequestHeader("X-USER-ROLE") String role,
                        @Valid @RequestBody AssignGrievanceRequest request) {

                return grievanceService
                                .assignGrievance(grievanceId, userId, role, request.getOfficerId());
                               
        }

        @PutMapping("/{grievanceId}/in-review")
        public Mono<Void> markInReview(
                        @PathVariable String grievanceId,
                        @RequestHeader("X-USER-ID") String userId,
                        @RequestHeader("X-USER-ROLE") String role) {

                return grievanceService.markInReview(grievanceId, userId, role);
        }

        @PutMapping("/{grievanceId}/resolve")
        public Mono<Void> resolveGrievance(
                        @PathVariable String grievanceId,
                        @RequestHeader("X-USER-ID") String userId,
                        @RequestHeader("X-USER-ROLE") String role) {

                return grievanceService
                                .resolveGrievance(grievanceId, userId, role);
        }

        @PutMapping("/{grievanceId}/close")
        public Mono<Void> closeGrievance(
                        @PathVariable String grievanceId,
                        @RequestHeader("X-USER-ID") String userId,
                        @RequestHeader("X-USER-ROLE") String role) {

                return grievanceService
                                .closeGrievance(grievanceId, userId, role);
        }

        @PutMapping("/{grievanceId}/reopen")
        public Mono<Void> reopen(
                        @PathVariable String grievanceId,
                        @RequestHeader("X-USER-ID") String userId,
                        @RequestHeader("X-USER-ROLE") String role) {

                return grievanceService.reopenGrievance(grievanceId, userId, role);
        }

        @GetMapping("/{grievanceId}/history")
        public Mono<ResponseEntity<List<GrievanceStatusHistory>>> history(
                @PathVariable String grievanceId,
                @RequestHeader("X-USER-ID") String userId,
                @RequestHeader("X-USER-ROLE") String role) {
            return grievanceService.getGrievanceHistory(grievanceId, userId, role)
                    .collectList()
                    .map(ResponseEntity::ok);
        }

        @GetMapping("/citizen/{citizenId}")
        public Flux<Grievance> getByCitizen(@PathVariable String citizenId,
                                            @RequestHeader("X-USER-ID") String userId,
                                            @RequestHeader("X-USER-ROLE") String role) {
                return grievanceService.getGrievancesByCitizen(citizenId, userId, role);
        }


        @GetMapping("/department/{departmentId}")
        public Flux<Grievance> getByDepartment(@PathVariable String departmentId,
                                               @RequestHeader("X-USER-ID") String userId,
                                               @RequestHeader("X-USER-ROLE") String role) {
                return grievanceService.getGrievancesByDepartment(departmentId, userId, role);
        }

        @PutMapping("/{grievanceId}/escalate")
        public Mono<Void> escalate(
                        @PathVariable String grievanceId,
                        @RequestHeader("X-USER-ID") String userId,
                        @RequestHeader("X-USER-ROLE") String role) {
                return grievanceService
                                .escalateGrievance(grievanceId, userId, role);
        }

        @GetMapping("/{grievanceId}")
        public Mono<ResponseEntity<Grievance>> getGrievanceById(@PathVariable String grievanceId, @RequestHeader("X-USER-ID") String userId,@RequestHeader("X-USER-ROLE") String role) {
                return grievanceService.getGrievanceById(grievanceId, userId, role)
                                .map(ResponseEntity::ok)
                                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
        }
        
        @GetMapping("/{grievanceId}/documents")
        public Flux<GrievanceDocument> getGrievanceDocuments(
                @PathVariable String grievanceId,
                @RequestHeader("X-USER-ID") String userId,
                @RequestHeader("X-USER-ROLE") String role) {
            return grievanceService.getGrievanceDocuments(grievanceId, userId, role);
        }

        @GetMapping("/{grievanceId}/documents/{documentId}")
        public Mono<ResponseEntity<Resource>> downloadDocument(
                @PathVariable String grievanceId,
                @PathVariable String documentId,
                @RequestHeader("X-USER-ID") String userId,
                @RequestHeader("X-USER-ROLE") String role) {
            
            return grievanceService.downloadDocument(grievanceId, documentId, userId, role)
                    .map(resource -> ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + resource.getFilename() + "\"")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource));
        }

}
