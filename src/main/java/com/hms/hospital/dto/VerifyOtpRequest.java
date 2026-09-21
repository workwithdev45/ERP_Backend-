package com.hms.hospital.dto;

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
public class VerifyOtpRequest {

    @NotBlank(message = "Admin email is required")
    @Email(message = "Please provide a valid email address")
    private String adminEmail;

    @NotBlank(message = "Please enter the 4-digit verification code")
    @Pattern(regexp = "^[0-9]{4}$", message = "OTP must be a 4-digit number")
    private String otp;
}

