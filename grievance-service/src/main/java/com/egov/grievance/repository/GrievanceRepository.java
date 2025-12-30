package com.egov.grievance.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.egov.grievance.model.Grievance;

import reactor.core.publisher.Flux;

public interface GrievanceRepository extends ReactiveMongoRepository<Grievance, String> {

	Flux<Grievance> findByCitizenId(String citizenId);

	Flux<Grievance> findByDepartmentId(String departmentId);
}
