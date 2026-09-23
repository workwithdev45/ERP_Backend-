package com.msmeerp.onboarding.dto;

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
public class FindCompanyRequest {

    @NotBlank(message = "User email is required")
    @Email(message = "Please provide a valid email address")
    private String userEmail;
}
