//package com.egov.grievance.service;
//
//import com.egov.grievance.config.DepartmentCategoryConfig;
//import com.egov.grievance.dto.CreateGrievanceRequest;
//import com.egov.grievance.dto.UserResponse;
//import com.egov.grievance.event.GrievanceStatusChangedEvent;
//import com.egov.grievance.exception.UserNotFoundException;
//import com.egov.grievance.model.GRIEVANCE_STATUS;
//import com.egov.grievance.model.Grievance;
//import com.egov.grievance.model.GrievanceDocument;
//import com.egov.grievance.repository.GrievanceDocumentRepository;
//import com.egov.grievance.repository.GrievanceHistoryRepository;
//import com.egov.grievance.repository.GrievanceRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
//import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
//import org.springframework.core.io.Resource;
//import org.springframework.http.codec.multipart.FilePart;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.ResponseStatusException;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.time.Instant;
//import java.util.function.Function;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class GrievanceServiceTest {
//
//    @Mock
//    private GrievanceRepository grievanceRepository;
//    @Mock
//    private GrievanceDocumentRepository grievanceDocumentRepository;
//    @Mock
//    private GrievanceHistoryService grievanceHistoryService;
//    @Mock
//    private ReferenceDataService referenceDataService;
//    @Mock
//    private WebClient.Builder webClientBuilder;
//    @Mock
//    private GrievanceEventPublisher grievanceEventPublisher;
//    @Mock
//    private GrievanceHistoryRepository grievanceHistoryRepository;
//    @Mock
//    private ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;
//
//    @Mock
//    private WebClient webClient;
//    @Mock
//    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
//    @Mock
//    private WebClient.RequestHeadersSpec requestHeadersSpec;
//    @Mock
//    private WebClient.ResponseSpec responseSpec;
//    @Mock
//    private ReactiveCircuitBreaker circuitBreaker;
//
//    private GrievanceService grievanceService;
//
//    @BeforeEach
//    void setUp() {
//        // Mock WebClient chain
//        lenient().when(webClientBuilder.build()).thenReturn(webClient);
//        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
//        lenient().when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
//        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
//
//        // Mock CircuitBreaker
//        lenient().when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
//        lenient().when(circuitBreaker.run(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
//
//        grievanceService = new GrievanceService(
//                grievanceRepository,
//                grievanceDocumentRepository,
//                grievanceHistoryService,
//                referenceDataService,
//                webClientBuilder,
//                grievanceEventPublisher,
//                grievanceHistoryRepository,
//                circuitBreakerFactory);
//    }
//
//    private Grievance makeGrievance(String id, String citizenId, String status) {
//        return Grievance.builder()
//                .id(id)
//                .citizenId(citizenId)
//                .departmentId("DEPT_01")
//                .categoryId("CAT_01")
//                .title("Issue Title")
//                .description("Issue Desc")
//                .status(GRIEVANCE_STATUS.valueOf(status))
//                .isEscalated(false)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//    }
//
//    // --- Create Grievance Tests ---
//
//    @Test
//    void createGrievance_Success() {
//        CreateGrievanceRequest req = new CreateGrievanceRequest();
//        req.setDepartmentId("DEPT_01");
//        req.setCategoryId("CAT_01");
//        req.setTitle("Bad Road");
//        req.setDescription("Fix it");
//
//        when(referenceDataService.validateDepartmentAndCategory("DEPT_01", "CAT_01")).thenReturn(Mono.empty());
//        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(i -> {
//            Grievance g = i.getArgument(0);
//            g.setId("G1");
//            return Mono.just(g);
//        });
//        when(grievanceHistoryService.createInitialHistory("G1", "USER1")).thenReturn(Mono.just("G1"));
//        doNothing().when(grievanceEventPublisher).publishStatusChange(any());
//
//        StepVerifier.create(grievanceService.createGrievance("USER1", "CITIZEN", req, Flux.empty()))
//                .expectNext("G1")
//                .verifyComplete();
//
//        verify(grievanceRepository).save(any(Grievance.class));
//        verify(grievanceHistoryService).createInitialHistory("G1", "USER1");
//    }
//
//    @Test
//    void createGrievance_InvalidRole() {
//        CreateGrievanceRequest req = new CreateGrievanceRequest();
//        StepVerifier.create(grievanceService.createGrievance("USER1", "ADMIN", req, Flux.empty()))
//                .expectErrorMatches(
//                        t -> t instanceof IllegalArgumentException && t.getMessage().contains("Only CITIZEN"))
//                .verify();
//    }
//
//    // --- Assign Grievance Tests ---
//
//    @Test
//    void assignGrievance_Success() {
//        Grievance grievance = makeGrievance("G1", "USER1", "SUBMITTED");
//        UserResponse officer = new UserResponse();
//        officer.setId("OFFICER1");
//        officer.setRole("OFFICER");
//        officer.setDepartmentId("DEPT_01");
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//
//        // Mock fetchUserById for Officer
//        String uri = "http://user-service/users/{id}";
//        when(requestHeadersUriSpec.uri(eq(uri), eq("OFFICER1"))).thenReturn(requestHeadersSpec);
//        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
//        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.just(officer));
//
//        when(grievanceRepository.save(any(Grievance.class))).thenReturn(Mono.just(grievance));
//        when(grievanceHistoryService.addHistory(anyString(), any(), any(), anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(grievanceService.assignGrievance("G1", "ADMIN1", "ADMIN", "OFFICER1"))
//                .verifyComplete();
//
//        assertEquals(GRIEVANCE_STATUS.ASSIGNED, grievance.getStatus());
//        assertEquals("OFFICER1", grievance.getAssignedOfficerId());
//    }
//
//    @Test
//    void assignGrievance_OfficerWrongDept() {
//        Grievance grievance = makeGrievance("G1", "USER1", "SUBMITTED");
//        UserResponse officer = new UserResponse();
//        officer.setId("OFFICER1");
//        officer.setRole("OFFICER");
//        officer.setDepartmentId("OTHER_DEPT");
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//
//        String uri = "http://user-service/users/{id}";
//        when(requestHeadersUriSpec.uri(eq(uri), eq("OFFICER1"))).thenReturn(requestHeadersSpec);
//        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
//        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.just(officer));
//
//        StepVerifier.create(grievanceService.assignGrievance("G1", "ADMIN1", "ADMIN", "OFFICER1"))
//                .expectErrorMatches(t -> t.getMessage().contains("Officer does not belong"))
//                .verify();
//    }
//
//    // --- Mark In Review Tests ---
//
//    @Test
//    void markInReview_Success() {
//        Grievance grievance = makeGrievance("G1", "USER1", "ASSIGNED");
//        grievance.setAssignedOfficerId("OFFICER1");
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//        when(grievanceRepository.save(any(Grievance.class))).thenReturn(Mono.just(grievance));
//        when(grievanceHistoryService.addHistory(anyString(), any(), any(), anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(grievanceService.markInReview("G1", "OFFICER1", "OFFICER"))
//                .verifyComplete();
//
//        assertEquals(GRIEVANCE_STATUS.IN_REVIEW, grievance.getStatus());
//    }
//
//    @Test
//    void markInReview_WrongOfficer() {
//        Grievance grievance = makeGrievance("G1", "USER1", "ASSIGNED");
//        grievance.setAssignedOfficerId("OFFICER1");
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//
//        StepVerifier.create(grievanceService.markInReview("G1", "OFFICER2", "OFFICER"))
//                .expectErrorMatches(t -> t.getMessage().contains("Officer not assigned"))
//                .verify();
//    }
//
//    // --- Resolve Grievance Tests ---
//
//    @Test
//    void resolveGrievance_Success() {
//        Grievance grievance = makeGrievance("G1", "USER1", "IN_REVIEW");
//        grievance.setAssignedOfficerId("OFFICER1");
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//        when(grievanceRepository.save(any(Grievance.class))).thenReturn(Mono.just(grievance));
//        when(grievanceHistoryService.addHistory(anyString(), any(), any(), anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(grievanceService.resolveGrievance("G1", "OFFICER1", "OFFICER"))
//                .verifyComplete();
//
//        assertEquals(GRIEVANCE_STATUS.RESOLVED, grievance.getStatus());
//        assertNotNull(grievance.getResolvedAt());
//    }
//
//    // --- Close Grievance Tests ---
//
//    @Test
//    void closeGrievance_Success() {
//        Grievance grievance = makeGrievance("G1", "USER1", "RESOLVED");
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//        when(grievanceRepository.save(any(Grievance.class))).thenReturn(Mono.just(grievance));
//        when(grievanceHistoryService.addHistory(anyString(), any(), any(), anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(grievanceService.closeGrievance("G1", "ADMIN1", "ADMIN"))
//                .verifyComplete();
//
//        assertEquals(GRIEVANCE_STATUS.CLOSED, grievance.getStatus());
//    }
//
//    // --- Reopen Grievance Tests ---
//
//    @Test
//    void reopenGrievance_Success() {
//        Grievance grievance = makeGrievance("G1", "USER1", "CLOSED");
//        grievance.setResolvedAt(Instant.now().minus(java.time.Duration.ofDays(2)));
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//        when(grievanceRepository.save(any(Grievance.class))).thenReturn(Mono.just(grievance));
//        when(grievanceHistoryService.addHistory(anyString(), any(), any(), anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(grievanceService.reopenGrievance("G1", "USER1", "CITIZEN"))
//                .verifyComplete();
//
//        assertEquals(GRIEVANCE_STATUS.REOPENED, grievance.getStatus());
//        assertFalse(grievance.getIsEscalated());
//        assertNull(grievance.getAssignedOfficerId());
//    }
//
//    @Test
//    void reopenGrievance_WindowExpired() {
//        Grievance grievance = makeGrievance("G1", "USER1", "CLOSED");
//        grievance.setResolvedAt(Instant.now().minus(java.time.Duration.ofDays(8)));
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//
//        StepVerifier.create(grievanceService.reopenGrievance("G1", "USER1", "CITIZEN"))
//                .expectErrorMatches(t -> t.getMessage().contains("Reopen window expired"))
//                .verify();
//    }
//
//    // --- Escalate Grievance Tests ---
//
//    @Test
//    void escalateGrievance_Success() {
//        Grievance grievance = makeGrievance("G1", "USER1", "SUBMITTED");
//        // Created 5 hours ago
//        grievance.setCreatedAt(Instant.now().minus(java.time.Duration.ofHours(5)));
//
//        when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//        // SLA is 2 hours
//        when(referenceDataService.getSlaHours(anyString(), anyString())).thenReturn(Mono.just(2));
//
//        // Mock supervisor ID call
//        String supervisorUri = "http://user-service/users/supervisor/department/{departmentId}";
//        when(requestHeadersUriSpec.uri(eq(supervisorUri), anyString())).thenReturn(requestHeadersSpec);
//        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("SUPERVISOR1"));
//
//        when(grievanceRepository.save(any(Grievance.class))).thenReturn(Mono.just(grievance));
//        when(grievanceHistoryService.addHistory(anyString(), any(), any(), anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(grievanceService.escalateGrievance("G1", "USER1", "CITIZEN"))
//                .verifyComplete();
//
//        assertEquals(GRIEVANCE_STATUS.ESCALATED, grievance.getStatus());
//        assertTrue(grievance.getIsEscalated());
//        assertEquals("SUPERVISOR1", grievance.getAssignedOfficerId());
//    }
//
//    // --- Document Download Tests ---
//
//    @Test
//    void downloadDocument_Success() {
//        Grievance grievance = makeGrievance("G1", "USER1", "SUBMITTED");
//        GrievanceDocument doc = new GrievanceDocument();
//        doc.setId("DOC1");
//        doc.setGrievanceId("G1");
//        // Create a temporary file to simulating existing file
//        try {
//            java.io.File temp = java.io.File.createTempFile("test", "txt");
//            doc.setFilePath(temp.getAbsolutePath());
//
//            when(grievanceRepository.findById("G1")).thenReturn(Mono.just(grievance));
//            when(grievanceDocumentRepository.findById("DOC1")).thenReturn(Mono.just(doc));
//
//            StepVerifier.create(grievanceService.downloadDocument("G1", "DOC1", "USER1", "CITIZEN"))
//                    .expectNextMatches(Resource::exists)
//                    .verifyComplete();
//
//            temp.deleteOnExit();
//        } catch (Exception e) {
//            fail("Failed to create temp file: " + e.getMessage());
//        }
//    }
//}
