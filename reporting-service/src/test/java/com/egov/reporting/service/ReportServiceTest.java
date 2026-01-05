//package com.egov.reporting.service;
//
//import com.egov.reporting.client.GrievanceClient;
//import com.egov.reporting.client.UserClient;
//import com.egov.reporting.dto.GrievanceDTO;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.web.server.ResponseStatusException;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class ReportServiceTest {
//
//    @Mock
//    private GrievanceClient grievanceClient;
//
//    @Mock
//    private UserClient userClient;
//
//    @InjectMocks
//    private ReportService reportService;
//
//    private GrievanceDTO g1, g2;
//
//    @BeforeEach
//    void setUp() {
//        Instant now = Instant.now();
//        g1 = new GrievanceDTO();
//        g1.setDepartmentId("DEPT1");
//        g1.setStatus("RESOLVED");
//        g1.setCreatedAt(now.minus(60, ChronoUnit.MINUTES));
//        g1.setResolvedAt(now.minus(30, ChronoUnit.MINUTES)); // took 30 mins
//
//        g2 = new GrievanceDTO();
//        g2.setDepartmentId("DEPT2");
//        g2.setStatus("CLOSED");
//        g2.setCreatedAt(now.minus(120, ChronoUnit.MINUTES));
//        g2.setResolvedAt(now.minus(30, ChronoUnit.MINUTES)); // took 90 mins
//    }
//
//    @Test
//    void getAverageResolutionTime_Success() {
//        when(grievanceClient.getGrievances(any(), any(), any(), any()))
//                .thenReturn(Flux.just(g1, g2));
//
//        StepVerifier.create(reportService.getAverageResolutionTime("u1", "ADMIN", null))
//                .assertNext(result -> {
//                    // (30 + 90) / 2 = 60
//                    assert result.get("averageTime").equals(60.0);
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void getAverageResolutionTime_NoData_Throws404() {
//        when(grievanceClient.getGrievances(any(), any(), any(), any()))
//                .thenReturn(Flux.empty());
//
//        StepVerifier.create(reportService.getAverageResolutionTime("u1", "ADMIN", null))
//                .expectError(ResponseStatusException.class)
//                .verify();
//    }
//
//    @Test
//    void getDepartmentPerformance_Success() {
//        g2.setDepartmentId("DEPT1"); // Both now in DEPT1
//        when(grievanceClient.getGrievances(any(), any(), any(), any()))
//                .thenReturn(Flux.just(g1, g2));
//
//        StepVerifier.create(reportService.getDepartmentPerformance("u1", "ADMIN"))
//                .assertNext(result -> {
//                    assert result.get("DEPT1") == 2;
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void getUserGrievanceSummary_UserNotFound() {
//        when(userClient.validateUserExists(anyString(), anyString(), anyString()))
//                .thenReturn(Mono.just(false));
//
//        StepVerifier.create(reportService.getUserGrievanceSummary("target", "requester", "ADMIN"))
//                .expectErrorMatches(ex -> ((ResponseStatusException)ex).getStatusCode().is4xxClientError())
//                .verify();
//    }
//}

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

}
