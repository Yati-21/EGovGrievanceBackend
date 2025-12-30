package com.egov.grievance.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "egov_grievances")
public class Grievance {

    @Id
    private String id;

    @NotBlank(message = "Citizen ID is mandatory")
    private String citizenId;

    @NotBlank(message = "Department ID is mandatory")
    private String departmentId;

    @NotBlank(message = "Category ID is mandatory")
    private String categoryId;

    @NotBlank(message = "Title is mandatory")
    private String title;

    @NotBlank(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Status is mandatory")
    private GRIEVANCE_STATUS status;

    private Boolean isEscalated = false;

    private String assignedOfficerId;

    @NotNull(message = "Created time is mandatory")
    private Instant createdAt;

    private Instant updatedAt;

    private Instant resolvedAt;
}
