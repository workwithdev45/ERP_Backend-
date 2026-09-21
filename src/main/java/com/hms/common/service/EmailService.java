package com.hms.common.service;

import java.util.List;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp);

    void sendWelcomeEmail(String toEmail, String portalId, String portalUrl);

    void sendHospitalLinksEmail(String toEmail, List<String> portalUrls);
}
