package com.egov.reporting.controller;

import com.egov.reporting.dto.GrievanceDTO;
import com.egov.reporting.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReportService reportService;

    @Test
    void getAvgResolutionTime_EndpointTest() {
        when(reportService.getAverageResolutionTime(any(), any(), any()))
                .thenReturn(Mono.just(Map.of("averageTime", 45.0, "unit", "minutes")));

        webTestClient.get()
                .uri("/reports/avg-resolution-time")
                .header("X-USER-ID", "user123")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.averageTime").isEqualTo(45.0);
    }

    @Test
    void getSlaBreaches_EndpointTest() {
        GrievanceDTO dto = new GrievanceDTO();
        dto.setId("GRV-1");

        when(reportService.getSlaBreaches(any(), any()))
                .thenReturn(Flux.just(dto));

        webTestClient.get()
                .uri("/reports/grievances/sla-breaches")
                .header("X-USER-ID", "user123")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GrievanceDTO.class)
                .hasSize(1);
    }

    @Test
    void getGrievancesByStatus_Test() {
        when(reportService.getGrievances(any(), any(), eq("OPEN"), isNull()))
                .thenReturn(Flux.just(new GrievanceDTO()));

        webTestClient.get()
                .uri("/reports/grievances/status/OPEN")
                .header("X-USER-ID", "U1")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GrievanceDTO.class)
                .hasSize(1);
    }

    @Test
    void getGrievancesByDepartment_Test() {
        when(reportService.getGrievances(any(), any(), isNull(), eq("D1")))
                .thenReturn(Flux.just(new GrievanceDTO()));

        webTestClient.get()
                .uri("/reports/grievances/department/D1")
                .header("X-USER-ID", "U1")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GrievanceDTO.class)
                .hasSize(1);
    }

    @Test
    void getDeptAvgResolutionTime_Test() {
        when(reportService.getAverageResolutionTime(any(), any(), eq("D1")))
                .thenReturn(Mono.just(Map.of("averageTime", 30.0)));

        webTestClient.get()
                .uri("/reports/avg-resolution-time/department/D1")
                .header("X-USER-ID", "U1")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.averageTime").isEqualTo(30.0);
    }

    @Test
    void getDepartmentPerformance_Test() {
        when(reportService.getDepartmentPerformance(any(), any()))
                .thenReturn(Mono.just(Map.of("D1", 5)));

        webTestClient.get()
                .uri("/reports/department-performance")
                .header("X-USER-ID", "U1")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.D1").isEqualTo(5);
    }

    @Test
    void getUserSummary_Test() {
        when(reportService.getUserGrievanceSummary(eq("U2"), any(), any()))
                .thenReturn(Mono.just(Map.of("OPEN", 2)));

        webTestClient.get()
                .uri("/reports/user/U2")
                .header("X-USER-ID", "ADMIN1")
                .header("X-USER-ROLE", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.OPEN").isEqualTo(2);
    }
    @Test
    void getPublicStats_EndpointTest() {
        Map<String, Object> stats = Map.of(
            "resolvedCount", 10,
            "resolutionRate", 85.5,
            "avgResolutionTime", 4.2
        );

        when(reportService.getPublicStats()).thenReturn(Mono.just(stats));

        webTestClient.get()
                .uri("/reports/public/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resolvedCount").isEqualTo(10)
                .jsonPath("$.resolutionRate").isEqualTo(85.5);
    }
}
