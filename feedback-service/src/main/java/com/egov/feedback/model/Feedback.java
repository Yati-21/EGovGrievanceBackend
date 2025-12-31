package com.egov.feedback.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Document(collection = "egov_feedback")
public class Feedback {

    @Id
    private String id;

    @NotBlank(message = "Grievance ID is mandatory")
    private String grievanceId;

    @NotBlank(message = "Citizen ID is mandatory")
    private String citizenId;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private int rating;

    private String comment;

    @NotNull(message = "Created time is mandatory")
    private Instant createdAt;
}
