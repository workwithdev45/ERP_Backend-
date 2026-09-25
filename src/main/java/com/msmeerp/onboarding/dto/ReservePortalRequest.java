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
public class ReservePortalRequest {

    @NotBlank(message = "Admin email is required")
    @Email(message = "Please provide a valid email address")
    private String adminEmail;

    @NotBlank(message = "Registration token is required")
    private String registrationToken;

    @NotBlank(message = "Workspace ID is required")
    @Size(min = 3, max = 50, message = "Workspace ID must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Workspace ID must contain only lowercase letters, numbers, and hyphens (without leading/trailing hyphens)")
    private String portalId;

    @Builder.Default
    private boolean startBlank = false;

    /** G10: Trader / Manufacturer / Services — used to preset which modules are switched on. */
    @Pattern(regexp = "^$|TRADER|MANUFACTURER|SERVICES", message = "Unrecognized business type")
    private String businessType;
}
