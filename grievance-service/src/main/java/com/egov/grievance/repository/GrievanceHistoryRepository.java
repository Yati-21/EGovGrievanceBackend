package com.egov.grievance.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.egov.grievance.model.GrievanceStatusHistory;

import reactor.core.publisher.Flux;

public interface GrievanceHistoryRepository extends ReactiveMongoRepository<GrievanceStatusHistory, String> {
	Flux<GrievanceStatusHistory> findByGrievanceId(String grievanceId);
}