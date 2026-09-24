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
import com.msmeerp.onboarding.dto.WorkspaceSummary;

import java.util.List;

public interface OnboardingService {

    ApiResponse<Void> register(CompanyRegisterInitRequest request);

    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    CheckPortalIdResponse checkPortalId(String portalId);

    ApiResponse<Void> reservePortal(ReservePortalRequest request);

    SetAdminPasswordResponse setAdminPassword(SetAdminPasswordRequest request);

    ApiResponse<List<WorkspaceSummary>> findCompanies(FindCompanyRequest request);
}
