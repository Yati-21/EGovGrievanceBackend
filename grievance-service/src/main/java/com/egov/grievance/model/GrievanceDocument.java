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
@Document(collection = "egov_grievance_documents")
public class GrievanceDocument 
{
    @Id
    private String id;
    private String grievanceId;
    private String uploadedBy;
    private String fileName;
    private String fileType;
    private String filePath;
    private Instant uploadedAt;
}
