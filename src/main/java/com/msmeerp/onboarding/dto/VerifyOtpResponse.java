package com.msmeerp.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpResponse {

    @Builder.Default
    private boolean error = false;

    private String message;

    private String registrationToken;

    /**
     * Set only when this signup already reserved a workspace in an earlier, abandoned attempt
     * (G1) — the client should skip straight to "set password" for this workspace instead of
     * claiming a new one.
     */
    private String existingPortalId;
}
