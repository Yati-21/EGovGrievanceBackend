package com.egov.grievance.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.egov.grievance.service.ReferenceDataService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/reference")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }
    
    @GetMapping("/departments")
    public Mono<ResponseEntity<Map<String, Map<String, Object>>>> getAllDepartments() {
        return referenceDataService.getAllDepartments()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/departments/{departmentId}/categories")
    public Mono<ResponseEntity<Map<String, Object>>> getCategories(@PathVariable String departmentId) {
        return referenceDataService.getCategoriesByDepartment(departmentId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/departments/{departmentId}/validate")
    public Mono<ResponseEntity<Void>> validateDepartment(
            @PathVariable String departmentId) {

        return referenceDataService
                .validateDepartmentOnly(departmentId)
                .thenReturn(ResponseEntity.ok().build());
    }
}
