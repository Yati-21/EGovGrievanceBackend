package com.egov.grievance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateGrievanceRequest {

    @NotBlank(message = "Department ID is mandatory")
    private String departmentId;

    @NotBlank(message = "Category ID is mandatory")
    private String categoryId;

    @NotBlank(message = "Title is mandatory")
    @Size(min = 3, max = 50, message = "title must be between 3 to 50 characters long")
    private String title;

    @NotBlank(message = "Description is mandatory")
    @Size(min = 3, max = 500, message = "description must be between 3 to 500 characters long")
    private String description;
}