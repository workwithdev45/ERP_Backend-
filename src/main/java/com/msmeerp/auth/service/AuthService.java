package com.msmeerp.auth.service;

import com.msmeerp.auth.dto.LoginRequest;
import com.msmeerp.auth.dto.LoginResponse;
import com.msmeerp.auth.dto.RefreshTokenRequest;
import com.msmeerp.auth.dto.UserProfileResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    UserProfileResponse getCurrentUserProfile();
    LoginResponse refreshToken(RefreshTokenRequest request);
}
