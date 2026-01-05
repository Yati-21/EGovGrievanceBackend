package com.egov.grievance.controller;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.egov.grievance.service.ReferenceDataService;

import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ReferenceDataController.class, properties = { "spring.cloud.config.enabled=false",
		"spring.config.import=optional:configserver:" })
class ReferenceDataControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ReferenceDataService service;

	@Test
	void getDepartments_success() {
		when(service.getAllDepartments()).thenReturn(Mono.just(Map.of("D1", Map.of("name", "Water"))));
		webTestClient.get().uri("/reference/departments").exchange().expectStatus().isOk();
	}

	@Test
	void getDepartments_notFound() {
		when(service.getAllDepartments()).thenReturn(Mono.empty());
		webTestClient.get().uri("/reference/departments").exchange().expectStatus().isNotFound();
	}

	@Test
	void getCategories_success() {
		when(service.getCategoriesByDepartment("D001")).thenReturn(Mono.just(Map.of("C1", "Water Leakage")));
		webTestClient.get().uri("/reference/departments/D001/categories").exchange().expectStatus().isOk();
	}

	@Test
	void validateDepartment_success() {
		when(service.validateDepartmentOnly("D001")).thenReturn(Mono.empty());
		webTestClient.get().uri("/reference/departments/D001/validate").exchange().expectStatus().isOk();
	}
}
