package com.egov.user.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
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
@Document(collection = "egov_users")
public class User {

    @Id
    private String id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @NotBlank(message = "Password hash cannot be empty")
    private String passwordHash;

    @NotNull(message = "Role is mandatory")
    private ROLE role;

    //can be null for citizen and admin , mandatory for officer and supervisor
    private String departmentId;

    @NotNull(message = "Created timestamp is mandatory")
    private Instant createdAt;
}