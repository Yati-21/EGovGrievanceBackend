package com.egov.grievance.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "egov_grievance_status_history")
public class GrievanceStatusHistory {

    @Id
    private String id;

    private String grievanceId;

    private GRIEVANCE_STATUS oldStatus;

    private GRIEVANCE_STATUS newStatus;

    private String changedBy;

    private Instant changedAt;
}
