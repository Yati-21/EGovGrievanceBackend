package com.egov.notification.event;

import java.time.Instant;

import lombok.Data;

@Data
public class GrievanceStatusChangedEvent {

    private String grievanceId;
    private String citizenId;
    private String departmentId;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private Instant changedAt;
}
