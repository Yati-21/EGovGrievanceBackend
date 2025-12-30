package com.egov.grievance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignGrievanceRequest {

    @NotBlank(message = "Officer ID is mandatory")
    private String officerId;
}
