package com.egov.grievance.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceStatusChangedEvent {

    private String grievanceId;
    private String citizenId;
    private String departmentId;

    private String oldStatus;
    private String newStatus;

    private String changedBy;   //userId
    private Instant changedAt;
}
