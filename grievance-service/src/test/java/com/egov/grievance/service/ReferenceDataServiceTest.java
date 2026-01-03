package com.egov.grievance.service;

import com.egov.grievance.config.DepartmentCategoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    @Mock
    private DepartmentCategoryConfig config;

    private ReferenceDataService service;

    @BeforeEach
    void setUp() {
        service = new ReferenceDataService(config);
    }

    @Test
    void getAllDepartments_Success() {
        when(config.getDepartments()).thenReturn(Map.of("DEPT_01", Map.of()));
        StepVerifier.create(service.getAllDepartments())
                .expectNextMatches(map -> map.containsKey("DEPT_01"))
                .verifyComplete();
    }

    @Test
    void getCategoriesByDepartment_Success() {
        when(config.isValidDepartment("DEPT_01")).thenReturn(true);
        when(config.getCategories("DEPT_01")).thenReturn(Map.of("CAT_01", Map.of()));

        StepVerifier.create(service.getCategoriesByDepartment("DEPT_01"))
                .expectNextMatches(map -> map.containsKey("CAT_01"))
                .verifyComplete();
    }

    @Test
    void getCategoriesByDepartment_NotFound() {
        when(config.isValidDepartment("DEPT_01")).thenReturn(false);
        StepVerifier.create(service.getCategoriesByDepartment("DEPT_01"))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void validateDepartmentAndCategory_Success() {
        when(config.isValid("DEPT_01", "CAT_01")).thenReturn(true);
        StepVerifier.create(service.validateDepartmentAndCategory("DEPT_01", "CAT_01"))
                .verifyComplete();
    }

    @Test
    void validateDepartmentAndCategory_Invalid() {
        when(config.isValid("DEPT_01", "CAT_01")).thenReturn(false);
        StepVerifier.create(service.validateDepartmentAndCategory("DEPT_01", "CAT_01"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void getSlaHours_Success() {
        when(config.getSlaHours("DEPT_01", "CAT_01")).thenReturn(48);
        StepVerifier.create(service.getSlaHours("DEPT_01", "CAT_01"))
                .expectNext(48)
                .verifyComplete();
    }
}
