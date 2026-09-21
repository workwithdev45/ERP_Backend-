package com.hms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String tenantId;
    private String tenantName;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String branchId;
    private String branchName;
    private String status;
    private Set<String> roles;
    private Set<String> permissions;
}
