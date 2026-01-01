package com.egov.notification.dto;

import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String role;
    private String departmentId;
}
