package com.egov.grievance.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.egov.grievance.model.GrievanceDocument;

import reactor.core.publisher.Flux;

public interface GrievanceDocumentRepository extends ReactiveMongoRepository<GrievanceDocument, String> {
    Flux<GrievanceDocument> findByGrievanceId(String grievanceId);
}
