package com.egov.grievance.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.egov.grievance.dto.CreateGrievanceRequest;
import com.egov.grievance.dto.UserResponse;
import com.egov.grievance.event.GrievanceStatusChangedEvent;
import com.egov.grievance.model.GRIEVANCE_STATUS;
import com.egov.grievance.model.Grievance;
import com.egov.grievance.model.GrievanceDocument;
import com.egov.grievance.model.GrievanceStatusHistory;
import com.egov.grievance.repository.GrievanceDocumentRepository;
import com.egov.grievance.repository.GrievanceHistoryRepository;
import com.egov.grievance.repository.GrievanceRepository;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GrievanceServiceTest {

	@Mock
	private GrievanceRepository grievanceRepository;
	@Mock
	private GrievanceDocumentRepository documentRepository;
	@Mock
	private GrievanceHistoryService historyService;
	@Mock
	private ReferenceDataService referenceDataService;
	@Mock
	private WebClient.Builder webClientBuilder;
	@Mock
	private GrievanceEventPublisher publisher;
	@Mock
	private GrievanceHistoryRepository historyRepository;
	@Mock
	private ReactiveCircuitBreakerFactory<?, ?> cbFactory;
	@Mock
	private ReactiveCircuitBreaker cb;

	@InjectMocks
	private GrievanceService service;

	@BeforeEach
	void setup() {
		lenient().when(cbFactory.create(anyString())).thenReturn(cb);

		lenient().when(cb.run(any(Mono.class), any())).thenAnswer(inv -> inv.getArgument(0));
	}

	private Grievance grievance(String status) {
		return Grievance.builder().id("G1").citizenId("U1").departmentId("D001").categoryId("C101").title("Leak")
				.description("Pipe leak").status(GRIEVANCE_STATUS.valueOf(status))
				.createdAt(Instant.now().minus(Duration.ofHours(100))).updatedAt(Instant.now()).isEscalated(false)
				.build();
	}

	@Test
	void createGrievance_success() {
		CreateGrievanceRequest req = new CreateGrievanceRequest();
		req.setDepartmentId("D001");
		req.setCategoryId("C101");
		req.setTitle("Leak");
		req.setDescription("Pipe");

		when(referenceDataService.validateDepartmentAndCategory(any(), any())).thenReturn(Mono.empty());

		when(grievanceRepository.save(any())).thenAnswer(inv -> {
			Grievance g = inv.getArgument(0);
			g.setId("G1");
			return Mono.just(g);
		});
		when(historyService.createInitialHistory("G1", "U1")).thenReturn(Mono.just("G1"));
		StepVerifier.create(service.createGrievance("U1", "CITIZEN", req, Flux.empty())).expectNext("G1")
				.verifyComplete();
		verify(publisher).publishStatusChange(any(GrievanceStatusChangedEvent.class));
	}

	@Test
	void createGrievance_nonCitizen() {
		StepVerifier.create(service.createGrievance("U1", "ADMIN", new CreateGrievanceRequest(), Flux.empty()))
				.expectError(IllegalArgumentException.class).verify();
	}

	// ---------------- ASSIGN ----------------

	@Test
	void assignGrievance_adminSuccess() {
		Grievance g = grievance("SUBMITTED");

		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(grievanceRepository.save(any())).thenReturn(Mono.just(g));
		when(historyService.addHistory(any(), any(), any(), any())).thenReturn(Mono.empty());

		mockUser("O1", "OFFICER", "D001");

		StepVerifier.create(service.assignGrievance("G1", "ADMIN", "ADMIN", "O1")).verifyComplete();
	}

	@Test
	void assignGrievance_wrongRole() {
		StepVerifier.create(service.assignGrievance("G1", "U1", "CITIZEN", "O1"))
				.expectError(IllegalArgumentException.class).verify();
	}

	// ---------------- MARK IN REVIEW ----------------

	@Test
	void markInReview_success() {
		Grievance g = grievance("ASSIGNED");
		g.setAssignedOfficerId("O1");

		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(grievanceRepository.save(any())).thenReturn(Mono.just(g));
		when(historyService.addHistory(any(), any(), any(), any())).thenReturn(Mono.empty());

		StepVerifier.create(service.markInReview("G1", "O1", "OFFICER")).verifyComplete();
	}

	@Test
	void resolveGrievance_success() {
		Grievance g = grievance("IN_REVIEW");
		g.setAssignedOfficerId("O1");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(grievanceRepository.save(any())).thenReturn(Mono.just(g));
		when(historyService.addHistory(any(), any(), any(), any())).thenReturn(Mono.empty());

		StepVerifier.create(service.resolveGrievance("G1", "O1", "OFFICER")).verifyComplete();
	}

	@Test
	void escalate_success() {
		Grievance g = grievance("SUBMITTED");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(referenceDataService.getSlaHours(any(), any())).thenReturn(Mono.just(1));
		mockSupervisor("S1", "D001");
		when(grievanceRepository.save(any())).thenReturn(Mono.just(g));
		when(historyService.addHistory(any(), any(), any(), any())).thenReturn(Mono.empty());
		StepVerifier.create(service.escalateGrievance("G1", "U1", "CITIZEN")).verifyComplete();
	}

	private void mockUser(String id, String role, String dept) {
		WebClient webClient = mock(WebClient.class);
		WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

		when(webClientBuilder.build()).thenReturn(webClient);
		when(webClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(eq("http://user-service/users/{id}"), any(Object[].class))).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(UserResponse.class))
				.thenReturn(Mono.just(new UserResponse(id, null, null, role, dept)));
	}

	private void mockSupervisor(String supervisorId, String departmentId) {
		WebClient webClient = mock(WebClient.class);
		WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		when(webClientBuilder.build()).thenReturn(webClient);
		when(webClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(eq("http://user-service/users/supervisor/department/{departmentId}"), any(Object[].class)))
				.thenReturn(headersSpec);
		when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(supervisorId));
	}

	@Test
	void getGrievances_admin_all() {
		when(grievanceRepository.findAll()).thenReturn(Flux.just(new Grievance()));
		StepVerifier.create(service.getGrievances(null, null, "ADMIN", "admin")).expectNextCount(1).verifyComplete();
	}

	@Test
	void getGrievances_citizen_onlyOwn() {
		when(grievanceRepository.findByCitizenId("u1")).thenReturn(Flux.just(new Grievance()));
		StepVerifier.create(service.getGrievances(null, null, "CITIZEN", "u1")).expectNextCount(1).verifyComplete();
	}

	@Test
	void getGrievanceById_admin_ok() {
		Grievance g = grievance("SUBMITTED");

		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.getGrievanceById("G1", "admin", "ADMIN")).expectNext(g).verifyComplete();
	}

	@Test
	void getGrievanceById_citizen_forbidden() {
		Grievance g = grievance("SUBMITTED");
		g.setCitizenId("OTHER");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));

		StepVerifier.create(service.getGrievanceById("G1", "u1", "CITIZEN")).expectError(ResponseStatusException.class)
				.verify();
	}

	@Test
	void closeGrievance_success() {
		Grievance g = grievance("RESOLVED");
		g.setAssignedOfficerId("O1");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(grievanceRepository.save(any())).thenReturn(Mono.just(g));
		when(historyService.addHistory(any(), any(), any(), any())).thenReturn(Mono.empty());

		StepVerifier.create(service.closeGrievance("G1", "O1", "OFFICER")).verifyComplete();
	}

	@Test
	void reopenGrievance_success() {
		Grievance g = grievance("CLOSED");
		g.setCitizenId("U1");
		g.setResolvedAt(Instant.now().minus(Duration.ofDays(2)));
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(grievanceRepository.save(any())).thenReturn(Mono.just(g));
		when(historyService.addHistory(any(), any(), any(), any())).thenReturn(Mono.empty());
		StepVerifier.create(service.reopenGrievance("G1", "U1", "CITIZEN")).verifyComplete();
	}

	@Test
	void getSlaBreaches_admin() {
		Grievance g = grievance("SUBMITTED");
		g.setCreatedAt(Instant.now().minus(Duration.ofHours(100)));
		when(grievanceRepository.findAll()).thenReturn(Flux.just(g));
		when(referenceDataService.getSlaHours(any(), any())).thenReturn(Mono.just(24));
		StepVerifier.create(service.getSlaBreaches("admin", "ADMIN")).expectNextCount(1).verifyComplete();
	}

	@Test
	void getGrievanceHistory_ok() {
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance("SUBMITTED")));
		when(historyRepository.findByGrievanceId("G1")).thenReturn(Flux.just(new GrievanceStatusHistory()));
		StepVerifier.create(service.getGrievanceHistory("G1", "admin", "ADMIN")).expectNextCount(1).verifyComplete();
	}

	@Test
	void downloadDocument_notFound() {
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance("SUBMITTED")));
		when(documentRepository.findById("D1")).thenReturn(Mono.empty());
		StepVerifier.create(service.downloadDocument("G1", "D1", "admin", "ADMIN"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getGrievancesByCitizen_admin_success() {
		when(grievanceRepository.findByCitizenId("U1")).thenReturn(Flux.just(new Grievance()));

		StepVerifier.create(service.getGrievancesByCitizen("U1", "admin", "ADMIN")).expectNextCount(1).verifyComplete();
	}

	@Test
	void getGrievancesByCitizen_citizen_forbidden() {
		StepVerifier.create(service.getGrievancesByCitizen("U1", "U2", "CITIZEN"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getGrievancesByCitizen_citizen_notFound() {
		when(grievanceRepository.findByCitizenId("U1")).thenReturn(Flux.empty());

		StepVerifier.create(service.getGrievancesByCitizen("U1", "U1", "CITIZEN"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getGrievancesByDepartment_admin_success() {
		when(referenceDataService.validateDepartmentOnly("D001")).thenReturn(Mono.empty());
		when(grievanceRepository.findByDepartmentId("D001")).thenReturn(Flux.just(new Grievance()));

		StepVerifier.create(service.getGrievancesByDepartment("D001", "admin", "ADMIN")).expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void getGrievancesByDepartment_supervisor_forbidden() {
		mockUser("S1", "SUPERVISOR", "D002");
		StepVerifier.create(service.getGrievancesByDepartment("D001", "S1", "SUPERVISOR"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void getGrievanceDocuments_success() {
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance("SUBMITTED")));
		when(documentRepository.findByGrievanceId("G1")).thenReturn(Flux.just(new GrievanceDocument()));
		StepVerifier.create(service.getGrievanceDocuments("G1", "admin", "ADMIN")).expectNextCount(1).verifyComplete();
	}

	@Test
	void createGrievance_withFile_uploadsSuccessfully() {
		FilePart filePart = mock(FilePart.class);
		when(filePart.filename()).thenReturn("test.txt");
		when(filePart.transferTo(any(Path.class))).thenReturn(Mono.empty());
		when(filePart.headers()).thenReturn(new org.springframework.http.HttpHeaders());
		when(referenceDataService.validateDepartmentAndCategory(any(), any())).thenReturn(Mono.empty());
		when(grievanceRepository.save(any())).thenAnswer(inv -> {
			Grievance g = inv.getArgument(0);
			g.setId("G1");
			return Mono.just(g);
		});

		when(documentRepository.save(any())).thenReturn(Mono.just(new GrievanceDocument()));
		when(historyService.createInitialHistory(any(), any())).thenReturn(Mono.just("G1"));
		StepVerifier.create(service.createGrievance("U1", "CITIZEN",
				new CreateGrievanceRequest("D001", "C101", "t", "d"), Flux.just(filePart))).expectNext("G1")
				.verifyComplete();
	}

	@Test
	void getGrievances_admin_withStatus() {
		when(grievanceRepository.findByStatus(GRIEVANCE_STATUS.SUBMITTED)).thenReturn(Flux.just(new Grievance()));
		StepVerifier.create(service.getGrievances("SUBMITTED", null, "ADMIN", "admin")).expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void getGrievances_admin_withDepartmentAndStatus() {
		when(referenceDataService.validateDepartmentOnly("D001")).thenReturn(Mono.empty());
		when(grievanceRepository.findByDepartmentIdAndStatus("D001", GRIEVANCE_STATUS.SUBMITTED))
				.thenReturn(Flux.just(new Grievance()));
		StepVerifier.create(service.getGrievances("SUBMITTED", "D001", "ADMIN", "admin")).expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void getGrievances_supervisor_filtersByDept() {
		mockUser("S1", "SUPERVISOR", "D001");
		when(grievanceRepository.findByDepartmentId("D001")).thenReturn(Flux.just(new Grievance()));
		StepVerifier.create(service.getGrievances(null, null, "SUPERVISOR", "S1")).expectNextCount(1).verifyComplete();
	}

	@Test
	void getGrievances_officer_onlyAssigned() {
		mockUser("O1", "OFFICER", "D001");
		when(grievanceRepository.findByAssignedOfficerId("O1")).thenReturn(Flux.just(new Grievance()));
		StepVerifier.create(service.getGrievances(null, null, "OFFICER", "O1")).expectNextCount(1).verifyComplete();
	}

	@Test
	void getGrievances_citizen_withStatus() {
		when(grievanceRepository.findByCitizenIdAndStatus("U1", GRIEVANCE_STATUS.SUBMITTED))
				.thenReturn(Flux.just(new Grievance()));
		StepVerifier.create(service.getGrievances("SUBMITTED", null, "CITIZEN", "U1")).expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void downloadDocument_wrongGrievance() {
		Grievance g = grievance("SUBMITTED");
		GrievanceDocument doc = GrievanceDocument.builder().id("D1").grievanceId("OTHER").filePath("fake").build();
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(documentRepository.findById("D1")).thenReturn(Mono.just(doc));
		StepVerifier.create(service.downloadDocument("G1", "D1", "admin", "ADMIN"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void assignGrievance_officerNotOfficerRole() {
		Grievance g = grievance("SUBMITTED");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		mockUser("U2", "CITIZEN", "D001"); // NOT OFFICER
		StepVerifier.create(service.assignGrievance("G1", "ADMIN", "ADMIN", "U2"))
				.expectError(IllegalArgumentException.class).verify();
	}

	@Test
	void assignGrievance_officerWrongDepartment() {
		Grievance g = grievance("SUBMITTED");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		mockUser("O1", "OFFICER", "D999"); // wrong dept
		StepVerifier.create(service.assignGrievance("G1", "ADMIN", "ADMIN", "O1"))
				.expectError(IllegalArgumentException.class).verify();
	}

	@Test
	void downloadDocument_fileMissingOnDisk() {
		Grievance g = grievance("SUBMITTED");
		GrievanceDocument doc = GrievanceDocument.builder().id("D1").grievanceId("G1").filePath("non-existing-path.txt")
				.build();
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(documentRepository.findById("D1")).thenReturn(Mono.just(doc));
		StepVerifier.create(service.downloadDocument("G1", "D1", "admin", "ADMIN"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void escalateGrievance_slaNotBreached() {
		Grievance g = grievance("SUBMITTED");
		g.setCreatedAt(Instant.now().minus(Duration.ofHours(1)));
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		when(referenceDataService.getSlaHours(any(), any())).thenReturn(Mono.just(48));
		StepVerifier.create(service.escalateGrievance("G1", "U1", "CITIZEN"))
				.expectError(IllegalArgumentException.class).verify();
	}

	@Test
	void getSlaBreaches_alreadyEscalated() {
		Grievance g = grievance("SUBMITTED");
		g.setIsEscalated(true);
		when(grievanceRepository.findAll()).thenReturn(Flux.just(g));
		StepVerifier.create(service.getSlaBreaches("admin", "ADMIN")).expectNextCount(1).verifyComplete();
	}

	@Test
	void assignGrievance_invalidStatus() {
		Grievance g = grievance("RESOLVED");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.assignGrievance("G1", "ADMIN", "ADMIN", "O1"))
				.expectError(IllegalArgumentException.class).verify();
	}

	@Test
	void assignGrievance_supervisor_wrongDepartment() {
		Grievance g = grievance("SUBMITTED");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		mockUser("S1", "SUPERVISOR", "OTHER_DEPT");
		StepVerifier.create(service.assignGrievance("G1", "S1", "SUPERVISOR", "O1"))
				.expectError(ResponseStatusException.class).verify();
	}

	@Test
	void markInReview_wrongRole() {
		StepVerifier.create(service.markInReview("G1", "O1", "ADMIN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void markInReview_officerNotAssigned() {
		Grievance g = grievance("ASSIGNED");
		g.setAssignedOfficerId("OTHER");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.markInReview("G1", "O1", "OFFICER")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void markInReview_wrongStatus() {
		Grievance g = grievance("SUBMITTED");
		g.setAssignedOfficerId("O1");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.markInReview("G1", "O1", "OFFICER")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void resolveGrievance_wrongRole() {
		StepVerifier.create(service.resolveGrievance("G1", "O1", "ADMIN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void resolveGrievance_notAssigned() {
		Grievance g = grievance("IN_REVIEW");
		g.setAssignedOfficerId("OTHER");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.resolveGrievance("G1", "O1", "OFFICER")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void resolveGrievance_wrongStatus() {
		Grievance g = grievance("ASSIGNED");
		g.setAssignedOfficerId("O1");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.resolveGrievance("G1", "O1", "OFFICER")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void closeGrievance_wrongRole() {
		StepVerifier.create(service.closeGrievance("G1", "U1", "CITIZEN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void closeGrievance_officerNotOwner() {
		Grievance g = grievance("RESOLVED");
		g.setAssignedOfficerId("OTHER");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.closeGrievance("G1", "O1", "OFFICER")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void closeGrievance_wrongStatus() {
		Grievance g = grievance("IN_REVIEW");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.closeGrievance("G1", "ADMIN", "ADMIN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void reopenGrievance_wrongRole() {
		StepVerifier.create(service.reopenGrievance("G1", "U1", "ADMIN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void reopenGrievance_notOwner() {
		Grievance g = grievance("CLOSED");
		g.setCitizenId("OTHER");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.reopenGrievance("G1", "U1", "CITIZEN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void reopenGrievance_missingResolvedAt() {
		Grievance g = grievance("CLOSED");
		g.setResolvedAt(null);
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.reopenGrievance("G1", "U1", "CITIZEN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void reopenGrievance_expiredWindow() {
		Grievance g = grievance("CLOSED");
		g.setResolvedAt(Instant.now().minus(Duration.ofDays(10)));
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.reopenGrievance("G1", "U1", "CITIZEN")).expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	void getGrievanceById_officer_forbidden() {
		Grievance g = grievance("SUBMITTED");
		g.setAssignedOfficerId("OTHER");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		StepVerifier.create(service.getGrievanceById("G1", "O1", "OFFICER")).expectError(ResponseStatusException.class)
				.verify();
	}

	@Test
	void getGrievanceById_supervisor_forbidden() {
		Grievance g = grievance("SUBMITTED");
		g.setDepartmentId("D002");
		when(grievanceRepository.findById("G1")).thenReturn(Mono.just(g));
		mockUser("S1", "SUPERVISOR", "D001");
		StepVerifier.create(service.getGrievanceById("G1", "S1", "SUPERVISOR"))
				.expectError(ResponseStatusException.class).verify();
	}

}
