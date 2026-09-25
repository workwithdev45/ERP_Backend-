package com.msmeerp.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetAdminPasswordResponse {

    @Builder.Default
    private boolean error = false;

    private String message;

    private String portalUrl;

    /**
     * Lets the client sign the new admin straight in (G2) instead of sending them to the login
     * form — shaped like {@code LoginResponse} so the frontend can feed it straight into the same
     * session-storage helper.
     */
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private String tenantId;
    private String tenantName;
    private String username;
    private String email;
    private Set<String> roles;
    private Set<String> permissions;
}
