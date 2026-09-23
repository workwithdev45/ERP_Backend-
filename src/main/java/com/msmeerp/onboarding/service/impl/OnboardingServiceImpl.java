package com.msmeerp.onboarding.service.impl;

import com.msmeerp.auth.security.JwtTokenProvider;
import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.common.service.EmailService;
import com.msmeerp.onboarding.dto.CheckPortalIdResponse;
import com.msmeerp.onboarding.dto.CompanyRegisterInitRequest;
import com.msmeerp.onboarding.dto.FindCompanyRequest;
import com.msmeerp.onboarding.dto.ReservePortalRequest;
import com.msmeerp.onboarding.dto.SetAdminPasswordRequest;
import com.msmeerp.onboarding.dto.SetAdminPasswordResponse;
import com.msmeerp.onboarding.dto.VerifyOtpRequest;
import com.msmeerp.onboarding.dto.VerifyOtpResponse;
import com.msmeerp.onboarding.entity.CompanyOnboarding;
import com.msmeerp.onboarding.repository.CompanyOnboardingRepository;
import com.msmeerp.onboarding.service.OnboardingService;
import com.msmeerp.onboarding.service.TenantProvisioningService;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.entity.UserTenantMap;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.tenant.repository.UserTenantMapRepository;
import com.msmeerp.user.entity.User;
import com.msmeerp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final CompanyOnboardingRepository onboardingRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UserTenantMapRepository userTenantMapRepository;
    private final TenantProvisioningService provisioningService;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.portal.base-domain:msmeerp.com}")
    private String baseDomain;

    @Value("${app.portal.scheme:https}")
    private String scheme;

    private static final Set<String> RESERVED_WORDS = Set.of(
            "admin", "administrator", "api", "app", "auth", "billing", "cdn",
            "dashboard", "dev", "dns", "doc", "docs", "erp", "ftp", "help",
            "host", "mail", "msme", "msmeerp", "portal", "root",
            "secure", "server", "signup", "ssl", "stage", "staging", "status",
            "superadmin", "support", "sysadmin", "system", "test", "web",
            "webmail", "www"
    );

    private static final Pattern PORTAL_ID_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ApiResponse<Void> register(CompanyRegisterInitRequest request) {
        String adminEmail = request.getAdminEmail().toLowerCase().trim();

        // 1. Check if this email already owns an activated company tenant
        Optional<Tenant> existingTenant = tenantRepository.findByAdminEmail(adminEmail);
        if (existingTenant.isPresent() && existingTenant.get().isActive()) {
            log.info("Registration rejected: admin {} already owns active company {}", adminEmail, existingTenant.get().getPortalId());
            throw new BadRequestException("A company portal '" + existingTenant.get().getPortalId() + "' is already registered with this email. Please sign in instead.");
        }

        // 2. Generate secure 4-digit OTP
        int randomCode = 1000 + secureRandom.nextInt(9000);
        String otp = String.valueOf(randomCode);
        long otpValidUntil = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes TTL

        // 3. Upsert CompanyOnboarding Record
        CompanyOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
                .orElse(CompanyOnboarding.builder()
                        .adminEmail(adminEmail)
                        .build());

        onboarding.setAdminPhone(request.getAdminPhone());
        onboarding.setOtp(otp);
        onboarding.setOtpValidUntil(otpValidUntil);
        onboarding.setOtpAttempts(0);
        onboarding.setLastOtpSentAt(Instant.now());
        onboarding.setStatus(CompanyOnboarding.OnboardingStatus.PENDING);
        onboardingRepository.save(onboarding);

        // 4. Send OTP email
        emailService.sendOtpEmail(adminEmail, otp);

        log.info("OTP generated and email sent to: {}", adminEmail);

        return ApiResponse.success("Verification code sent to your email. Please verify the OTP to continue registration.");
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String adminEmail = request.getAdminEmail().toLowerCase().trim();

        // 1. Find pending onboarding record
        CompanyOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please start registration first."));

        // 2. Rate limiting check (max 5 attempts)
        if (onboarding.getOtpAttempts() != null && onboarding.getOtpAttempts() >= 5) {
            throw new BadRequestException("Too many incorrect attempts. Please request a new verification code.");
        }

        // 3. Expiry check (10 min TTL)
        if (onboarding.getOtpValidUntil() == null || onboarding.getOtpValidUntil() < System.currentTimeMillis()) {
            throw new BadRequestException("Your code has expired, please request a new one");
        }

        // 4. Match 4-digit OTP
        if (!StringUtils.hasText(request.getOtp()) || !request.getOtp().trim().equals(onboarding.getOtp())) {
            onboarding.setOtpAttempts((onboarding.getOtpAttempts() == null ? 0 : onboarding.getOtpAttempts()) + 1);
            onboardingRepository.save(onboarding);
            throw new BadRequestException("That code wasn't correct");
        }

        // 5. Mint short-lived registration JWT token
        String registrationToken = tokenProvider.generateRegistrationToken(adminEmail, onboarding.getOnboardingId());

        onboarding.setStatus(CompanyOnboarding.OnboardingStatus.EMAIL_VERIFIED);
        onboarding.setRegistrationToken(registrationToken);
        onboarding.setOtpAttempts(0);
        onboardingRepository.save(onboarding);

        return VerifyOtpResponse.builder()
                .error(false)
                .message("Email verified successfully.")
                .registrationToken(registrationToken)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckPortalIdResponse checkPortalId(String candidatePortalId) {
        if (!StringUtils.hasText(candidatePortalId)) {
            return CheckPortalIdResponse.builder()
                    .error(false)
                    .available(false)
                    .message("Portal ID is required")
                    .build();
        }

        String portalId = candidatePortalId.toLowerCase().trim();

        // Check length and format
        if (portalId.length() < 3 || portalId.length() > 50 || !PORTAL_ID_PATTERN.matcher(portalId).matches()) {
            return CheckPortalIdResponse.builder()
                    .error(false)
                    .available(false)
                    .message("Portal ID must be 3-50 characters with only lowercase letters, digits, and hyphens")
                    .build();
        }

        // Check reserved list
        if (RESERVED_WORDS.contains(portalId)) {
            return CheckPortalIdResponse.builder()
                    .error(false)
                    .available(false)
                    .message("'" + portalId + "' is a reserved name")
                    .build();
        }

        // Check uniqueness in database
        if (tenantRepository.existsByPortalId(portalId) || tenantRepository.existsById(portalId)) {
            return CheckPortalIdResponse.builder()
                    .error(false)
                    .available(false)
                    .message("Portal ID is already taken by another company")
                    .build();
        }

        return CheckPortalIdResponse.builder()
                .error(false)
                .available(true)
                .message("")
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Void> reservePortal(ReservePortalRequest request) {
        String adminEmail = request.getAdminEmail().toLowerCase().trim();
        String portalId = request.getPortalId().toLowerCase().trim();

        // 1. Validate registration token
        if (!tokenProvider.validateRegistrationToken(request.getRegistrationToken(), adminEmail)) {
            throw new BadRequestException("Registration session invalid or expired. Please verify your email again.");
        }

        // 2. Validate onboarding state
        CompanyOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Registration session invalid or expired. Please verify your email again."));

        if (onboarding.getStatus() == CompanyOnboarding.OnboardingStatus.PENDING) {
            throw new BadRequestException("Please verify your email OTP before reserving a portal.");
        }

        // 3. Race condition protection: re-validate portal ID
        CheckPortalIdResponse check = checkPortalId(portalId);
        if (!check.isAvailable()) {
            throw new BadRequestException(check.getMessage());
        }

        // 4. Provision dedicated ERP tenant
        provisioningService.provisionTenant(adminEmail, portalId, request.isStartBlank());

        // 5. Update onboarding record
        onboarding.setPortalId(portalId);
        onboarding.setStatus(CompanyOnboarding.OnboardingStatus.PORTAL_RESERVED);
        onboardingRepository.save(onboarding);

        return ApiResponse.success("Company portal reserved and provisioned successfully!");
    }

    @Override
    @Transactional
    public SetAdminPasswordResponse setAdminPassword(SetAdminPasswordRequest request) {
        String adminEmail = request.getAdminEmail().toLowerCase().trim();
        String portalId = request.getPortalId().toLowerCase().trim();

        // 1. Validate registration token
        if (!tokenProvider.validateRegistrationToken(request.getRegistrationToken(), adminEmail)) {
            throw new BadRequestException("Registration session invalid or expired. Please verify your email again.");
        }

        // 2. Validate onboarding state
        CompanyOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Registration session invalid or expired. Please verify your email again."));

        if (onboarding.getStatus() != CompanyOnboarding.OnboardingStatus.PORTAL_RESERVED
                || !portalId.equalsIgnoreCase(onboarding.getPortalId())) {
            throw new BadRequestException("Portal must be reserved before setting admin password.");
        }

        // 3. Find Super Admin user for this tenant and update password
        Tenant tenant = tenantRepository.findByPortalId(portalId)
                .or(() -> tenantRepository.findById(portalId))
                .orElseThrow(() -> new BadRequestException("Company portal not found: " + portalId));

        User adminUser = userRepository.findByTenantIdAndEmail(tenant.getId(), adminEmail)
                .orElseGet(() -> {
                    String username = adminEmail.split("@")[0];
                    return userRepository.findByTenantIdAndUsername(tenant.getId(), username)
                            .orElseThrow(() -> new BadRequestException("Super Admin user record not found for tenant " + portalId));
                });

        adminUser.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        adminUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(adminUser);

        // 4. Mark onboarding ACTIVE
        onboarding.setStatus(CompanyOnboarding.OnboardingStatus.ACTIVE);
        onboardingRepository.save(onboarding);

        // 5. Construct live portal URL
        String portalUrl = "%s://%s.%s".formatted(scheme, portalId, baseDomain);

        // 6. Send welcome email
        emailService.sendWelcomeEmail(adminEmail, portalId, portalUrl);

        return SetAdminPasswordResponse.builder()
                .error(false)
                .message("Admin password set and your ERP workspace is ready!")
                .portalUrl(portalUrl)
                .build();
    }

    @Override
    public ApiResponse<Void> findCompanies(FindCompanyRequest request) {
        String userEmail = request.getUserEmail().toLowerCase().trim();

        // Look up portal IDs from UserTenantMap or Tenants
        Optional<UserTenantMap> mapOpt = userTenantMapRepository.findByEmail(userEmail);
        List<String> portalIds = new ArrayList<>();

        if (mapOpt.isPresent() && StringUtils.hasText(mapOpt.get().getTenantIds())) {
            portalIds.addAll(Arrays.asList(mapOpt.get().getTenantIds().split(",")));
        } else {
            Optional<Tenant> tenantOpt = tenantRepository.findByAdminEmail(userEmail);
            tenantOpt.ifPresent(t -> portalIds.add(t.getPortalId()));
        }

        if (portalIds.isEmpty()) {
            throw new BadRequestException("This email is not linked to any company portal.");
        }

        List<String> portalUrls = portalIds.stream()
                .map(id -> "%s://%s.%s".formatted(scheme, id.trim(), baseDomain))
                .toList();

        // Send links via email
        emailService.sendPortalLinksEmail(userEmail, portalUrls);

        return ApiResponse.success("Check your email for your company portal link(s).");
    }
}
