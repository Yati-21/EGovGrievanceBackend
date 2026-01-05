package com.egov.grievance.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.egov.grievance.dto.AssignGrievanceRequest;
import com.egov.grievance.model.Grievance;
import com.egov.grievance.model.GrievanceDocument;
import com.egov.grievance.model.GrievanceStatusHistory;
import com.egov.grievance.repository.GrievanceDocumentRepository;
import com.egov.grievance.repository.GrievanceHistoryRepository;
import com.egov.grievance.repository.GrievanceRepository;
import com.egov.grievance.service.GrievanceService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = GrievanceController.class, properties = { "spring.cloud.config.enabled=false",
		"spring.config.import=optional:configserver:" })
class GrievanceControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private GrievanceService grievanceService;
	@MockBean
	private GrievanceHistoryRepository grievanceHistoryRepository;

	@MockBean
	private GrievanceRepository grievanceRepository;

	@MockBean
	private GrievanceDocumentRepository grievanceDocumentRepository;

	@Test
	void getAllGrievances_admin() {
		when(grievanceService.getGrievances(null, null, "ADMIN", "admin")).thenReturn(Flux.just(new Grievance()));
		webTestClient.get().uri("/grievances").header("X-USER-ID", "admin").header("X-USER-ROLE", "ADMIN").exchange()
				.expectStatus().isOk();
	}

	@Test
	void getSlaBreaches_success() {
		when(grievanceService.getSlaBreaches("admin", "ADMIN")).thenReturn(Flux.just(new Grievance()));
		webTestClient.get().uri("/grievances/sla-breaches").header("X-USER-ID", "admin").header("X-USER-ROLE", "ADMIN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void assignGrievance_success() {
		AssignGrievanceRequest req = new AssignGrievanceRequest();
		req.setOfficerId("O1");

		when(grievanceService.assignGrievance(any(), any(), any(), any())).thenReturn(Mono.empty());
		webTestClient.put().uri("/grievances/G1/assign").header("X-USER-ID", "admin").header("X-USER-ROLE", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).bodyValue(req).exchange().expectStatus().isOk();
	}

	@Test
	void markInReview() {
		when(grievanceService.markInReview(any(), any(), any())).thenReturn(Mono.empty());
		webTestClient.put().uri("/grievances/G1/in-review").header("X-USER-ID", "O1").header("X-USER-ROLE", "OFFICER")
				.exchange().expectStatus().isOk();
	}

	@Test
	void resolveGrievance() {
		when(grievanceService.resolveGrievance(any(), any(), any())).thenReturn(Mono.empty());
		webTestClient.put().uri("/grievances/G1/resolve").header("X-USER-ID", "O1").header("X-USER-ROLE", "OFFICER")
				.exchange().expectStatus().isOk();
	}

	@Test
	void closeGrievance() {
		when(grievanceService.closeGrievance(any(), any(), any())).thenReturn(Mono.empty());
		webTestClient.put().uri("/grievances/G1/close").header("X-USER-ID", "O1").header("X-USER-ROLE", "OFFICER")
				.exchange().expectStatus().isOk();
	}

	@Test
	void reopenGrievance() {
		when(grievanceService.reopenGrievance(any(), any(), any())).thenReturn(Mono.empty());
		webTestClient.put().uri("/grievances/G1/reopen").header("X-USER-ID", "U1").header("X-USER-ROLE", "CITIZEN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void history_success() {
		when(grievanceService.getGrievanceHistory(any(), any(), any()))
				.thenReturn(Flux.just(new GrievanceStatusHistory()));
		webTestClient.get().uri("/grievances/G1/history").header("X-USER-ID", "admin").header("X-USER-ROLE", "ADMIN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void getByCitizen() {
		when(grievanceService.getGrievancesByCitizen(any(), any(), any())).thenReturn(Flux.just(new Grievance()));
		webTestClient.get().uri("/grievances/citizen/U1").header("X-USER-ID", "U1").header("X-USER-ROLE", "CITIZEN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void getByDepartment() {
		when(grievanceService.getGrievancesByDepartment(any(), any(), any())).thenReturn(Flux.just(new Grievance()));
		webTestClient.get().uri("/grievances/department/D1").header("X-USER-ID", "admin").header("X-USER-ROLE", "ADMIN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void escalate() {
		when(grievanceService.escalateGrievance(any(), any(), any())).thenReturn(Mono.empty());
		webTestClient.put().uri("/grievances/G1/escalate").header("X-USER-ID", "U1").header("X-USER-ROLE", "CITIZEN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void getGrievanceDocuments() {
		when(grievanceService.getGrievanceDocuments(any(), any(), any()))
				.thenReturn(Flux.just(new GrievanceDocument()));
		webTestClient.get().uri("/grievances/G1/documents").header("X-USER-ID", "admin").header("X-USER-ROLE", "ADMIN")
				.exchange().expectStatus().isOk();
	}

	@Test
	void downloadDocument() {
		when(grievanceService.downloadDocument(any(), any(), any(), any()))
				.thenReturn(Mono.just(new ByteArrayResource("test".getBytes())));
		webTestClient.get().uri("/grievances/G1/documents/D1").header("X-USER-ID", "admin")
				.header("X-USER-ROLE", "ADMIN").exchange().expectStatus().isOk();
	}

	@Test
	void getGrievanceById_notFound() {
		when(grievanceService.getGrievanceById(any(), any(), any())).thenReturn(Mono.empty());
		webTestClient.get().uri("/grievances/G1").header("X-USER-ID", "U1").header("X-USER-ROLE", "CITIZEN").exchange()
				.expectStatus().isNotFound();
	}
}
