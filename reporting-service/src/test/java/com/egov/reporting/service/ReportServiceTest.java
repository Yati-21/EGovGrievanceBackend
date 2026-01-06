package com.egov.reporting.service;

import com.egov.reporting.client.GrievanceClient;
import com.egov.reporting.client.UserClient;
import com.egov.reporting.dto.GrievanceDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

	@Mock
	private GrievanceClient grievanceClient;
	@Mock
	private UserClient userClient;

	@InjectMocks
	private ReportService reportService;

	@Test
	void getGrievances_notFound() {
		when(grievanceClient.getGrievances(any(), any(), any(), any())).thenReturn(Flux.empty());

		StepVerifier.create(reportService.getGrievances("U1", "ADMIN", "OPEN", null))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getAverageResolutionTime_success() {
		GrievanceDTO g = new GrievanceDTO();
		g.setCreatedAt(Instant.now().minusSeconds(600));
		g.setResolvedAt(Instant.now());

		when(grievanceClient.getGrievances(any(), any(), any(), any())).thenReturn(Flux.just(g));
		StepVerifier.create(reportService.getAverageResolutionTime("U1", "ADMIN", null))
				.expectNextMatches(map -> map.containsKey("averageTime")).verifyComplete();
	}

	@Test
	void getDepartmentPerformance_empty() {
		when(grievanceClient.getGrievances(any(), any(), any(), any())).thenReturn(Flux.empty());
		StepVerifier.create(reportService.getDepartmentPerformance("U1", "ADMIN"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getUserSummary_userNotFound() {
		when(userClient.validateUserExists(any(), any(), any())).thenReturn(Mono.just(false));
		StepVerifier.create(reportService.getUserGrievanceSummary("T1", "U1", "ADMIN"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getAverageResolutionTime_nanBranch() {
		GrievanceDTO g = new GrievanceDTO();
		g.setCreatedAt(Instant.now());
		g.setResolvedAt(null);
		when(grievanceClient.getGrievances(any(), any(), any(), any())).thenReturn(Flux.just(g));
		StepVerifier.create(reportService.getAverageResolutionTime("U1", "ADMIN", null))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getDepartmentPerformance_success() {
		GrievanceDTO g = new GrievanceDTO();
		g.setDepartmentId("D1");
		when(grievanceClient.getGrievances(any(), any(), any(), any())).thenReturn(Flux.just(g, g));
		StepVerifier.create(reportService.getDepartmentPerformance("U1", "ADMIN"))
				.expectNextMatches(map -> map.get("D1") == 2).verifyComplete();
	}

	@Test
	void getUserSummary_userExists_emptyResult() {
		GrievanceDTO g = new GrievanceDTO();
		g.setCitizenId("OTHER");
		when(userClient.validateUserExists(any(), any(), any())).thenReturn(Mono.just(true));
		when(grievanceClient.getGrievances(any(), any(), any(), any())).thenReturn(Flux.just(g));
		StepVerifier.create(reportService.getUserGrievanceSummary("TARGET", "REQ", "ADMIN")).expectNext(Map.of())
				.verifyComplete();
	}

	@Test
	void getUserSummary_success_withData() {
		GrievanceDTO g = new GrievanceDTO();
		g.setCitizenId("TARGET");
		g.setStatus("OPEN");
		when(userClient.validateUserExists(any(), any(), any())).thenReturn(Mono.just(true));
		when(grievanceClient.getGrievances(any(), any(), any(), any())).thenReturn(Flux.just(g, g));
		StepVerifier.create(reportService.getUserGrievanceSummary("TARGET", "REQ", "ADMIN"))
				.expectNextMatches(map -> map.get("OPEN") == 2).verifyComplete();
	}
	
	@Test
    void getPublicStats_success_withCalculations() {
        Instant now = Instant.now();
        GrievanceDTO g1 = new GrievanceDTO();
        g1.setStatus("RESOLVED");
        g1.setCreatedAt(now.minusSeconds(7200)); 
        g1.setResolvedAt(now);
        GrievanceDTO g2 = new GrievanceDTO();
        g2.setStatus("OPEN");
        when(grievanceClient.getGrievances("SYSTEM", "ADMIN", null, null))
                .thenReturn(Flux.just(g1, g2));

        StepVerifier.create(reportService.getPublicStats())
                .expectNextMatches(stats -> 
                    stats.get("resolvedCount").equals(1L) &&
                    stats.get("resolutionRate").equals(50.0) &&
                    stats.get("avgResolutionTime").equals(2.0)
                )
                .verifyComplete();
    }

    @Test
    void getPublicStats_emptyData_returnsZeroStats() {
        when(grievanceClient.getGrievances("SYSTEM", "ADMIN", null, null))
                .thenReturn(Flux.empty());
        StepVerifier.create(reportService.getPublicStats())
                .expectNextMatches(stats -> 
                    stats.get("resolvedCount").equals(0) &&
                    stats.get("resolutionRate").equals(0.0) &&
                    stats.get("avgResolutionTime").equals(0.0)
                )
                .verifyComplete();
    }

}
