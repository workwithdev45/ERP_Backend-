package com.msmeerp.onboarding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Portal ID is required")
    private String portalId;

    @NotBlank(message = "Registration token is required")
    private String registrationToken;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ".*[0-9].*", message = "Password must contain at least one digit")
    private String adminPassword;
}
