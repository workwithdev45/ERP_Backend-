package com.msmeerp.common.service.impl;

import com.msmeerp.common.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.List;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final SesClient sesClient;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailServiceImpl(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "MSME ERP confirmation code: " + otp;
        String body = """
                <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 20px;">Thank you for signing up for MSME ERP. We're happy you're here!</p>
                <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 24px;">Enter the following code in the window where you began setting up your company workspace:</p>
                <div style="background-color: #1e293b; padding: 20px; border-radius: 8px; text-align: center; margin: 0 0 24px;">
                    <span style="font-size: 34px; font-weight: 700; letter-spacing: 10px; color: #f8fafc; font-family: monospace;">%s</span>
                </div>
                <p style="color: #94a3b8; font-size: 13px; margin: 0;">This code expires in <strong>10 minutes</strong>. This email contains private information for your account — please don't forward it.</p>
                """.formatted(otp);

        sendEmail(toEmail, subject, wrapInBrandedShell(body));
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String portalId, String portalUrl) {
        String subject = "MSME ERP: your company workspace is live";
        String body = """
                <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 20px;">Congratulations! You've created the new MSME ERP company workspace <strong>%s</strong>. Here are your account details:</p>
                <div style="background-color: #1e293b; padding: 20px; border-radius: 8px; text-align: center; margin: 0 0 24px;">
                    <div style="color: #f8fafc; font-size: 18px; font-weight: 700; margin-bottom: 12px;">%s</div>
                    <div style="color: #94a3b8; font-size: 14px; margin-bottom: 6px;">URL: <a href="%s" style="color: #60a5fa; text-decoration: none;">%s</a></div>
                    <div style="color: #94a3b8; font-size: 14px;">Email: %s</div>
                </div>
                <div style="text-align: center; margin: 0 0 24px;">
                    <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 700; display: inline-block;">Access Your Workspace</a>
                </div>
                <p style="color: #94a3b8; font-size: 13px; margin: 0;">You can now sign in using your Admin email and the password you configured.</p>
                """.formatted(portalId, portalId, portalUrl, portalUrl, toEmail, portalUrl);

        sendEmail(toEmail, subject, wrapInBrandedShell(body));
    }

    @Override
    public void sendPortalLinksEmail(String toEmail, List<String> portalUrls) {
        String subject = "Your associated MSME ERP company workspaces";
        StringBuilder linksHtml = new StringBuilder();
        for (String url : portalUrls) {
            linksHtml.append("<li style=\"margin-bottom: 6px;\"><a href=\"").append(url)
                    .append("\" style=\"color: #60a5fa; text-decoration: none;\">").append(url).append("</a></li>");
        }

        String body = """
                <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 16px;">Here are the company workspaces linked to your email (%s):</p>
                <ul style="color: #cbd5e1; font-size: 15px; line-height: 1.6; margin: 0 0 24px; padding-left: 20px;">
                    %s
                </ul>
                <p style="color: #94a3b8; font-size: 13px; margin: 0;">If you have any questions, please contact your company administrator.</p>
                """.formatted(toEmail, linksHtml.toString());

        sendEmail(toEmail, subject, wrapInBrandedShell(body));
    }

    @Override
    public void sendUserInviteEmail(String toEmail, String companyName, String inviteUrl) {
        String subject = "You've been invited to join " + companyName + " on MSME ERP";
        String body = """
                <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 20px;"><strong>%s</strong> has invited you to join their MSME ERP workspace.</p>
                <div style="text-align: center; margin: 0 0 24px;">
                    <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 700; display: inline-block;">Accept invitation</a>
                </div>
                <p style="color: #94a3b8; font-size: 13px; margin: 0;">This invite link expires in 3 days. If you weren't expecting this, you can safely ignore this email.</p>
                """.formatted(companyName, inviteUrl);

        sendEmail(toEmail, subject, wrapInBrandedShell(body));
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String companyName, String resetUrl, long validMinutes) {
        String subject = "Reset your MSME ERP password";
        String body = """
                <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 20px;">We received a request to reset the password for your <strong>%s</strong> account on MSME ERP.</p>
                <div style="text-align: center; margin: 0 0 24px;">
                    <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 700; display: inline-block;">Reset password</a>
                </div>
                <p style="color: #94a3b8; font-size: 13px; margin: 0 0 8px;">This link expires in <strong>%d minutes</strong> and can be used once.</p>
                <p style="color: #94a3b8; font-size: 13px; margin: 0;">If you didn't ask to reset your password, you can ignore this email — your password won't change.</p>
                """.formatted(companyName, resetUrl, validMinutes);

        sendEmail(toEmail, subject, wrapInBrandedShell(body));
    }

    private String wrapInBrandedShell(String bodyHtml) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background-color: #0f172a; border-radius: 12px;">
                    <div style="margin-bottom: 24px;">
                        <span style="font-size: 20px; font-weight: 700; color: #f8fafc;">MSME</span>
                        <span style="font-size: 20px; font-weight: 700; color: #2563eb;"> ERP</span>
                    </div>
                    %s
                    <div style="margin-top: 32px; padding-top: 16px; border-top: 1px solid #1e293b; text-align: center;">
                        <span style="color: #64748b; font-size: 12px;">Made by MSME ERP</span>
                    </div>
                </div>
                """.formatted(bodyHtml);
    }

    private static final java.util.regex.Pattern HREF_PATTERN = java.util.regex.Pattern.compile("href=\"([^\"]+)\"");

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        if (!mailEnabled) {
            String plainText = htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            java.util.regex.Matcher linkMatcher = HREF_PATTERN.matcher(htmlContent);
            String links = linkMatcher.results()
                    .map(m -> m.group(1))
                    .distinct()
                    .reduce("", (a, b) -> a + "\n         " + b);
            log.info("\n======================== [EMAIL DEV MODE] ========================" +
                            "\nTo:      {}" +
                            "\nSubject: {}" +
                            "\nPayload: {}" +
                            "\nLink(s): {}" +
                            "\n====================================================================",
                    toEmail, subject, plainText, links.isBlank() ? "(none)" : links);
            return;
        }

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromAddress)
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlContent).build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("Email sent successfully via SES to: {}", toEmail);
        } catch (SesException e) {
            log.error("Failed to send email via SES to {}: {}", toEmail, e.awsErrorDetails().errorMessage(), e);
        }
    }
}
