package com.msmeerp.onboarding.service;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.onboarding.dto.CheckPortalIdResponse;
import com.msmeerp.onboarding.dto.CompanyRegisterInitRequest;
import com.msmeerp.onboarding.dto.FindCompanyRequest;
import com.msmeerp.onboarding.dto.ReservePortalRequest;
import com.msmeerp.onboarding.dto.SetAdminPasswordRequest;
import com.msmeerp.onboarding.dto.SetAdminPasswordResponse;
import com.msmeerp.onboarding.dto.VerifyOtpRequest;
import com.msmeerp.onboarding.dto.VerifyOtpResponse;

public interface OnboardingService {

    ApiResponse<Void> register(CompanyRegisterInitRequest request);

    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    CheckPortalIdResponse checkPortalId(String portalId);

    ApiResponse<Void> reservePortal(ReservePortalRequest request);

    SetAdminPasswordResponse setAdminPassword(SetAdminPasswordRequest request);

    /**
     * Always returns the same generic response regardless of whether the email is linked to any
     * workspace — the actual workspace links (if any) are emailed, never returned in the response,
     * so this endpoint can't be used to enumerate registered companies (G4).
     */
    ApiResponse<Void> findCompanies(FindCompanyRequest request);
}
