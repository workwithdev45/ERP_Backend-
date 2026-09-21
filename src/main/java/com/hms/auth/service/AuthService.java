package com.hms.auth.service;

import com.hms.auth.dto.LoginRequest;
import com.hms.auth.dto.LoginResponse;
import com.hms.auth.dto.RefreshTokenRequest;
import com.hms.auth.dto.UserProfileResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    UserProfileResponse getCurrentUserProfile();
    LoginResponse refreshToken(RefreshTokenRequest request);
}
