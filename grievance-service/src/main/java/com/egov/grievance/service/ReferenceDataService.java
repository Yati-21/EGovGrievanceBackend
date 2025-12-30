package com.egov.grievance.service;

import org.springframework.stereotype.Service;

import com.egov.grievance.config.DepartmentCategoryConfig;

import reactor.core.publisher.Mono;

@Service
public class ReferenceDataService {

    private final DepartmentCategoryConfig config;

    public ReferenceDataService(DepartmentCategoryConfig config) {
        this.config = config;
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
}
