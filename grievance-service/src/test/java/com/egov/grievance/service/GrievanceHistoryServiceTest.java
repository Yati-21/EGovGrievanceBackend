package com.egov.grievance.service;

import com.egov.grievance.model.GRIEVANCE_STATUS;
import com.egov.grievance.model.GrievanceStatusHistory;
import com.egov.grievance.repository.GrievanceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrievanceHistoryServiceTest {

    @Mock
    private GrievanceHistoryRepository repository;

    private GrievanceHistoryService service;

    @BeforeEach
    void setUp() {
        service = new GrievanceHistoryService(repository);
    }

    @Test
    void createInitialHistory_Success() {
        when(repository.save(any(GrievanceStatusHistory.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(service.createInitialHistory("G1", "USER1"))
                .expectNext("G1")
                .verifyComplete();

        verify(repository).save(any(GrievanceStatusHistory.class));
    }

    @Test
    void addHistory_Success() {
        when(repository.save(any(GrievanceStatusHistory.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(service.addHistory("G1", GRIEVANCE_STATUS.SUBMITTED, GRIEVANCE_STATUS.ASSIGNED, "ADMIN1"))
                .verifyComplete();

        verify(repository).save(any(GrievanceStatusHistory.class));
    }
}
