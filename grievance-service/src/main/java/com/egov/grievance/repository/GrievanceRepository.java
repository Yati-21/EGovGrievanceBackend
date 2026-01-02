package com.egov.grievance.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.egov.grievance.model.GRIEVANCE_STATUS;
import com.egov.grievance.model.Grievance;

import reactor.core.publisher.Flux;

public interface GrievanceRepository extends ReactiveMongoRepository<Grievance, String> {

	Flux<Grievance> findByCitizenId(String citizenId);

	Flux<Grievance> findByDepartmentId(String departmentId);
	
	Flux<Grievance> findByAssignedOfficerId(String officerId);

	Flux<Grievance> findByStatus(GRIEVANCE_STATUS status);
	
    Flux<Grievance> findByDepartmentIdAndStatus(String departmentId, GRIEVANCE_STATUS status);
    
    Flux<Grievance> findByAssignedOfficerIdAndStatus(String assignedOfficerId, GRIEVANCE_STATUS status);
	
	Flux<Grievance> findByCitizenIdAndStatus(String citizenId, GRIEVANCE_STATUS status);
}
