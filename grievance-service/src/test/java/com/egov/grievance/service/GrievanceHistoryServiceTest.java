package com.egov.grievance.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.egov.grievance.model.GrievanceStatusHistory;
import com.egov.grievance.repository.GrievanceHistoryRepository;

import org.junit.jupiter.api.extension.ExtendWith;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GrievanceHistoryServiceTest {

	@Mock
	private GrievanceHistoryRepository repository;

	@InjectMocks
	private GrievanceHistoryService service;

	@Test
	void createInitialHistory() {
		when(repository.save(any())).thenReturn(Mono.just(new GrievanceStatusHistory()));
		StepVerifier.create(service.createInitialHistory("g1", "u1")).expectNext("g1").verifyComplete();
	}

	@Test
	void addHistory() {
		when(repository.save(any())).thenReturn(Mono.just(new GrievanceStatusHistory()));
		StepVerifier.create(service.addHistory("g1", null, null, "u1")).verifyComplete();
	}
}
