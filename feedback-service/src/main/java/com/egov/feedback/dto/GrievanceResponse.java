package com.egov.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrievanceResponse {
    private String id;
    private String status;
    private String citizenId;
}
