package com.egov.reporting.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class GrievanceDTO {
    private String id;
    private String title;
    private String description;
    private String status;
    private String departmentId;
    private String categoryId;
    private String assignedOfficerId;
    private String citizenId;
    private boolean isEscalated;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
}