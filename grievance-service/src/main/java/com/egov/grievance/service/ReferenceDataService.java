package com.egov.grievance.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.egov.grievance.config.DepartmentCategoryConfig;

import reactor.core.publisher.Mono;

@Service
public class ReferenceDataService {

    private final DepartmentCategoryConfig config;

    public ReferenceDataService(DepartmentCategoryConfig config) {
        this.config = config;
    }

    public Mono<Map<String, Map<String, Object>>> getAllDepartments() {
        return Mono.justOrEmpty(config.getDepartments());
    }

    public Mono<Map<String, Object>> getCategoriesByDepartment(String departmentId) {
        if (!config.isValidDepartment(departmentId)) {
            return Mono.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Department not found: " + departmentId));
        }
        return Mono.just(config.getCategories(departmentId));
    }

    public Mono<Void> validateDepartmentAndCategory(
            String departmentId,
            String categoryId) 
    {
        boolean valid = config.isValid(departmentId, categoryId);

        if (!valid) 
        {
            return Mono.error(new IllegalArgumentException("Invalid department or category"));
        }
        return Mono.empty();
    }

    public Mono<Integer> getSlaHours(String departmentId, String categoryId) {
        return Mono.fromCallable(() -> config.getSlaHours(departmentId, categoryId));
    }

    public Mono<Void> validateDepartmentOnly(String departmentId) {

        boolean valid = config.isValidDepartment(departmentId);

        if (!valid) {
            return Mono.error(
                new IllegalArgumentException("Invalid department"));
        }
        return Mono.empty();
    }

}
