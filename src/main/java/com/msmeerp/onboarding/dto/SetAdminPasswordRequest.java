package com.msmeerp.onboarding.dto;

import com.msmeerp.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetAdminPasswordRequest {

    @NotBlank(message = "Admin email is required")
    @Email(message = "Please provide a valid email address")
    private String adminEmail;

    @NotBlank(message = "Workspace ID is required")
    private String portalId;

    @NotBlank(message = "Registration token is required")
    private String registrationToken;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String adminPassword;
}
