package com.hms.user.dto;

import com.hms.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
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
    private User.UserStatus status;
    private Set<String> roles;
    private Instant createdAt;
}
