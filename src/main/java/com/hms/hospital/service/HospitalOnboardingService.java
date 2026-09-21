package com.hms.hospital.service;

import com.hms.common.response.ApiResponse;
import com.hms.hospital.dto.CheckPortalIdResponse;
import com.hms.hospital.dto.FindHospitalRequest;
import com.hms.hospital.dto.HospitalRegisterInitRequest;
import com.hms.hospital.dto.ReservePortalRequest;
import com.hms.hospital.dto.SetAdminPasswordRequest;
import com.hms.hospital.dto.SetAdminPasswordResponse;
import com.hms.hospital.dto.VerifyOtpRequest;
import com.hms.hospital.dto.VerifyOtpResponse;

public interface HospitalOnboardingService {

    ApiResponse<Void> register(HospitalRegisterInitRequest request);

    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    CheckPortalIdResponse checkPortalId(String portalId);

    ApiResponse<Void> reservePortal(ReservePortalRequest request);

    SetAdminPasswordResponse setAdminPassword(SetAdminPasswordRequest request);

    ApiResponse<Void> findHospitals(FindHospitalRequest request);
}
