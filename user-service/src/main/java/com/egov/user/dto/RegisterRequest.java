package com.egov.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is mandatory")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 5, max = 50, message = "Password must be between 5 and 50 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,20}$",message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character")
    private String password;

    
	//Optional in request
	//Defaults-CITIZEN if not provided
    private String role; //role is String here, not enum (safer for APIs)


    //Mandatory only for OFFICER and SUPERVISOR
    private String departmentId;
}