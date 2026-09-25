package com.msmeerp.onboarding.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.onboarding.dto.CheckPortalIdResponse;
import com.msmeerp.onboarding.dto.CompanyRegisterInitRequest;
import com.msmeerp.onboarding.dto.FindCompanyRequest;
import com.msmeerp.onboarding.dto.ReservePortalRequest;
import com.msmeerp.onboarding.dto.SetAdminPasswordRequest;
import com.msmeerp.onboarding.dto.SetAdminPasswordResponse;
import com.msmeerp.onboarding.dto.VerifyOtpRequest;
import com.msmeerp.onboarding.dto.VerifyOtpResponse;
import com.msmeerp.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody CompanyRegisterInitRequest request) {
        ApiResponse<Void> response = onboardingService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = onboardingService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-portal-id/{portalId}")
    public ResponseEntity<CheckPortalIdResponse> checkPortalId(@PathVariable String portalId) {
        CheckPortalIdResponse response = onboardingService.checkPortalId(portalId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reserve-portal")
    public ResponseEntity<ApiResponse<Void>> reservePortal(@Valid @RequestBody ReservePortalRequest request) {
        ApiResponse<Void> response = onboardingService.reservePortal(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/set-admin-password")
    public ResponseEntity<SetAdminPasswordResponse> setAdminPassword(@Valid @RequestBody SetAdminPasswordRequest request) {
        SetAdminPasswordResponse response = onboardingService.setAdminPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/find")
    public ResponseEntity<ApiResponse<Void>> findCompanies(@Valid @RequestBody FindCompanyRequest request) {
        ApiResponse<Void> response = onboardingService.findCompanies(request);
        return ResponseEntity.ok(response);
    }
}
