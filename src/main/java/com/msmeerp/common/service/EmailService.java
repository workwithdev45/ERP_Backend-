package com.msmeerp.common.service;

import java.util.List;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp);

    void sendWelcomeEmail(String toEmail, String portalId, String portalUrl);

    void sendPortalLinksEmail(String toEmail, List<String> portalUrls);

    void sendUserInviteEmail(String toEmail, String companyName, String inviteUrl);

    void sendPasswordResetEmail(String toEmail, String companyName, String resetUrl, long validMinutes);
}
