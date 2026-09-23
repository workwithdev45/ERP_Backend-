package com.msmeerp.onboarding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRegisterInitRequest {

    @NotBlank(message = "Admin email is required")
    @Email(message = "Please provide a valid email address")
    private String adminEmail;

    @Pattern(regexp = "^$|^[+]?[0-9\\s-]{8,20}$", message = "Please provide a valid mobile number")
    private String adminPhone;
}
