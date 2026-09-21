package com.hms.hospital.controller;

import com.hms.common.response.ApiResponse;
import com.hms.hospital.dto.CheckPortalIdResponse;
import com.hms.hospital.dto.FindHospitalRequest;
import com.hms.hospital.dto.HospitalRegisterInitRequest;
import com.hms.hospital.dto.ReservePortalRequest;
import com.hms.hospital.dto.SetAdminPasswordRequest;
import com.hms.hospital.dto.SetAdminPasswordResponse;
import com.hms.hospital.dto.VerifyOtpRequest;
import com.hms.hospital.dto.VerifyOtpResponse;
import com.hms.hospital.service.HospitalOnboardingService;
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
@RequestMapping("/hospitals")
@RequiredArgsConstructor
public class HospitalOnboardingController {

    private final HospitalOnboardingService onboardingService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody HospitalRegisterInitRequest request) {
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
    public ResponseEntity<ApiResponse<Void>> findHospital(@Valid @RequestBody FindHospitalRequest request) {
        ApiResponse<Void> response = onboardingService.findHospitals(request);
        return ResponseEntity.ok(response);
    }
}
