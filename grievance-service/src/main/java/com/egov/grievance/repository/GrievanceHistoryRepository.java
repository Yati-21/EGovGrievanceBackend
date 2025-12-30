package com.egov.grievance.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.egov.grievance.model.GrievanceStatusHistory;

public interface GrievanceHistoryRepository extends ReactiveMongoRepository<GrievanceStatusHistory, String> {
}