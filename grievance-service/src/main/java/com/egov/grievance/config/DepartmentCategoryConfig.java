package com.egov.grievance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Component
public class DepartmentCategoryConfig {

    private Map<String, Map<String, Object>> departments;

    @PostConstruct
    public void load() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("departments-categories-sla.json").getInputStream();

            Map<String, Object> root = mapper.readValue(is, Map.class);
            this.departments = (Map<String, Map<String, Object>>) root.get("departments");

        } 
        catch (Exception e) {
            throw new RuntimeException("Failed to load department-category config", e);
        }
    }

    public boolean isValid(String departmentId, String categoryId) {
        if (departments == null) {
            return false;
        }
        return departments.containsKey(departmentId)
                && ((Map<?, ?>) departments.get(departmentId).get("categories"))
                   .containsKey(categoryId);
    }

    public boolean isValidDepartment(String departmentId) {
    	if (departments == null) {
    		return false;
    	}
    	return departments.containsKey(departmentId);
    }
    
    public Integer getSlaHours(String departmentId, String categoryId) {

        if (departments == null || !departments.containsKey(departmentId)) {
            throw new IllegalArgumentException("Invalid department");
        }

        Map<String, Object> dept = departments.get(departmentId);
        Map<String, Object> categories = (Map<String, Object>) dept.get("categories");

        if (!categories.containsKey(categoryId)) {
            throw new IllegalArgumentException("Invalid category");
        }

        Map<String, Object> category =
                (Map<String, Object>) categories.get(categoryId);

        return (Integer) category.get("slaHours");
    }




}
