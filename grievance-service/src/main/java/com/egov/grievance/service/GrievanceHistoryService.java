package com.egov.grievance.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.egov.grievance.model.GRIEVANCE_STATUS;
import com.egov.grievance.model.GrievanceStatusHistory;
import com.egov.grievance.repository.GrievanceHistoryRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GrievanceHistoryService {

    private final GrievanceHistoryRepository repository;

    public Mono<String> createInitialHistory(
            String grievanceId,
            String userId
    ) 
    {
        GrievanceStatusHistory history =
                GrievanceStatusHistory.builder()
                        .grievanceId(grievanceId)
                        .oldStatus(null)
                        .newStatus(GRIEVANCE_STATUS.SUBMITTED)
                        .changedBy(userId)
                        .changedAt(Instant.now())
                        .build();

        return repository.save(history)
                .thenReturn(grievanceId);
    }
}
