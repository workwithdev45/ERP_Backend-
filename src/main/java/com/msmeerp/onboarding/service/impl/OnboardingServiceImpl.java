package com.msmeerp.onboarding.service.impl;

import com.msmeerp.auth.security.JwtTokenProvider;
import com.msmeerp.auth.security.UserPrincipal;
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
import com.msmeerp.common.exception.TooManyRequestsException;
import com.msmeerp.common.ratelimit.RateLimiterService;
import com.msmeerp.common.util.PortalUrlBuilder;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.entity.UserTenantMap;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.tenant.repository.UserTenantMapRepository;
import com.msmeerp.user.entity.User;
import com.msmeerp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

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
    private final RateLimiterService rateLimiterService;
    private final PortalUrlBuilder portalUrlBuilder;

    private static final Set<String> RESERVED_WORDS = Set.of(
            "admin", "administrator", "api", "app", "auth", "billing", "cdn",
            "dashboard", "dev", "dns", "doc", "docs", "erp", "ftp", "help",
            "host", "mail", "msme", "msmeerp", "portal", "root",
            "secure", "server", "signup", "ssl", "stage", "staging", "status",
            "superadmin", "support", "sysadmin", "system", "test", "web",
            "webmail", "www"
    );

    private static final Pattern PORTAL_ID_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final String TERMS_VERSION = "2026-09-24";
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ApiResponse<Void> register(CompanyRegisterInitRequest request) {
        String adminEmail = request.getAdminEmail().toLowerCase().trim();

        if (!rateLimiterService.allow("otp-send:" + adminEmail, 5, Duration.ofMinutes(15))) {
            throw new TooManyRequestsException("Too many verification codes requested. Please wait a few minutes and try again.");
        }

        // 1. Check if this email already owns an activated company tenant
        Optional<Tenant> existingTenant = tenantRepository.findByAdminEmail(adminEmail);
        if (existingTenant.isPresent() && existingTenant.get().isActive()) {
            log.info("Registration rejected: admin {} already owns active company {}", adminEmail, existingTenant.get().getPortalId());
            throw new BadRequestException("A company workspace is already registered with this email. Please sign in instead.");
        }

        // 2. Generate secure 4-digit OTP
        int randomCode = 1000 + secureRandom.nextInt(9000);
        String otp = String.valueOf(randomCode);
        long otpValidUntil = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes TTL

        // 3. Upsert CompanyOnboarding Record
        Optional<CompanyOnboarding> existing = onboardingRepository.findByAdminEmail(adminEmail);
        CompanyOnboarding onboarding = existing.orElse(CompanyOnboarding.builder()
                .adminEmail(adminEmail)
                .build());

        // G11: only require ticking Terms on a genuinely new signup — resuming one (resending the
        // OTP, or re-registering after abandoning at "claim workspace", G1) already has it on file.
        if (onboarding.getTermsAcceptedAt() == null) {
            if (!request.isTermsAccepted()) {
                throw new BadRequestException("Please accept the Terms of Service and Privacy Policy to continue");
            }
            onboarding.setTermsAcceptedAt(Instant.now());
            onboarding.setTermsVersion(TERMS_VERSION);
        }

        onboarding.setAdminPhone(request.getAdminPhone());
        onboarding.setOtp(otp);
        onboarding.setOtpValidUntil(otpValidUntil);
        onboarding.setOtpAttempts(0);
        onboarding.setLastOtpSentAt(Instant.now());
        // G1: a resend/re-register must not regress a signup that already reserved a workspace
        // (PORTAL_RESERVED) back to PENDING — that would hide the "resume at set-password" case
        // that verifyOtp() relies on below. Only (re)start the funnel for signups that haven't
        // claimed a workspace yet.
        if (onboarding.getStatus() == null
                || onboarding.getStatus() == CompanyOnboarding.OnboardingStatus.PENDING
                || onboarding.getStatus() == CompanyOnboarding.OnboardingStatus.EMAIL_VERIFIED) {
            onboarding.setStatus(CompanyOnboarding.OnboardingStatus.PENDING);
        }
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

        // G1: if an earlier attempt already reserved a workspace for this email and provisioned
        // its tenant/admin, don't force them through "claim workspace" again — that would fail
        // since the workspace is already theirs. Let the resumed session skip straight to
        // "set password" for the workspace they already have, as long as it's still unclaimed
        // (an admin who finished setup wouldn't still be here — see the "already registered"
        // check in register()).
        boolean alreadyReserved = onboarding.getStatus() == CompanyOnboarding.OnboardingStatus.PORTAL_RESERVED
                && StringUtils.hasText(onboarding.getPortalId());

        if (!alreadyReserved) {
            onboarding.setStatus(CompanyOnboarding.OnboardingStatus.EMAIL_VERIFIED);
        }
        onboarding.setRegistrationToken(registrationToken);
        onboarding.setOtpAttempts(0);
        onboardingRepository.save(onboarding);

        return VerifyOtpResponse.builder()
                .error(false)
                .message("Email verified successfully.")
                .registrationToken(registrationToken)
                .existingPortalId(alreadyReserved ? onboarding.getPortalId() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckPortalIdResponse checkPortalId(String candidatePortalId) {
        if (!StringUtils.hasText(candidatePortalId)) {
            return CheckPortalIdResponse.builder()
                    .error(false)
                    .available(false)
                    .message("Workspace ID is required")
                    .build();
        }

        String portalId = candidatePortalId.toLowerCase().trim();

        // Check length and format
        if (portalId.length() < 3 || portalId.length() > 50 || !PORTAL_ID_PATTERN.matcher(portalId).matches()) {
            return CheckPortalIdResponse.builder()
                    .error(false)
                    .available(false)
                    .message("Workspace ID must be 3-50 characters with only lowercase letters, digits, and hyphens")
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

        // Check uniqueness in database — only ACTIVE tenants block reuse, so a workspace
        // released by the abandoned-signup cleanup job (G1) can be claimed again.
        if (tenantRepository.existsByPortalIdAndActiveTrue(portalId)) {
            return CheckPortalIdResponse.builder()
                    .error(false)
                    .available(false)
                    .message("Workspace ID is already taken by another company")
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
            throw new BadRequestException("Please verify your email OTP before reserving a workspace.");
        }

        // 3. Race condition protection: re-validate portal ID
        CheckPortalIdResponse check = checkPortalId(portalId);
        if (!check.isAvailable()) {
            throw new BadRequestException(check.getMessage());
        }

        // 4. Provision dedicated ERP tenant
        Tenant provisioned = provisioningService.provisionTenant(adminEmail, portalId, request.isStartBlank(), request.getBusinessType());

        // G11: carry the Terms & Privacy acceptance captured at sign-up onto the tenant record.
        if (onboarding.getTermsAcceptedAt() != null) {
            provisioned.setTermsAcceptedAt(onboarding.getTermsAcceptedAt());
            provisioned.setTermsVersion(onboarding.getTermsVersion());
            tenantRepository.save(provisioned);
        }

        // 5. Update onboarding record
        onboarding.setPortalId(portalId);
        onboarding.setStatus(CompanyOnboarding.OnboardingStatus.PORTAL_RESERVED);
        onboardingRepository.save(onboarding);

        return ApiResponse.success("Company workspace reserved and provisioned successfully!");
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
            throw new BadRequestException("Workspace must be reserved before setting admin password.");
        }

        // 3. Find Super Admin user for this tenant and update password
        Tenant tenant = tenantRepository.findByPortalId(portalId)
                .or(() -> tenantRepository.findById(portalId))
                .orElseThrow(() -> new BadRequestException("Company workspace not found: " + portalId));

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
        String portalUrl = portalUrlBuilder.originFor(portalId);

        // 6. Send welcome email
        emailService.sendWelcomeEmail(adminEmail, portalId, portalUrl);

        // 7. Sign them straight in (G2) — no reason to make a brand-new admin retype
        // their workspace ID and password they just chose.
        UserPrincipal principal = UserPrincipal.create(adminUser, Collections.emptyList(), Collections.emptyList());
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(adminUser.getUsername(), tenant.getId());

        Set<String> roles = adminUser.getRoles().stream()
                .map(role -> role.getName().toUpperCase())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> permissions = adminUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName().toUpperCase())
                .collect(java.util.stream.Collectors.toSet());
        String tenantName = StringUtils.hasText(tenant.getDisplayName()) ? tenant.getDisplayName() : tenant.getName();

        return SetAdminPasswordResponse.builder()
                .error(false)
                .message("Admin password set and your ERP workspace is ready!")
                .portalUrl(portalUrl)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .tenantId(tenant.getId())
                .tenantName(tenantName)
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public ApiResponse<Void> findCompanies(FindCompanyRequest request) {
        String userEmail = request.getUserEmail().toLowerCase().trim();
        String genericMessage = "If that email is linked to a company workspace, we've sent the sign-in link(s) to it.";

        if (!rateLimiterService.allow("find-workspace:" + userEmail, 5, Duration.ofHours(1))) {
            // Same generic response even when rate-limited, so the endpoint still can't be used
            // to probe whether an email exists based on response differences.
            return ApiResponse.success(genericMessage);
        }

        // Look up portal IDs from UserTenantMap or Tenants
        Optional<UserTenantMap> mapOpt = userTenantMapRepository.findByEmail(userEmail);
        List<String> portalIds = new ArrayList<>();

        // Workspaces the user was added to, plus the one they administer (either record may be missing).
        if (mapOpt.isPresent() && StringUtils.hasText(mapOpt.get().getTenantIds())) {
            portalIds.addAll(Arrays.asList(mapOpt.get().getTenantIds().split(",")));
        }
        tenantRepository.findByAdminEmail(userEmail)
                .map(Tenant::getPortalId)
                .ifPresent(portalIds::add);

        List<String> portalUrls = portalIds.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .filter(portalId -> tenantRepository.findByPortalId(portalId).map(Tenant::isActive).orElse(false))
                .map(portalUrlBuilder::originFor)
                .toList();

        // Deliberately never reveal via the response whether the email matched anything (G4) —
        // only ever send matches via email. Logging keeps this observable to us, not the caller.
        if (portalUrls.isEmpty()) {
            log.info("Find-workspace requested for an email with no linked workspace");
        } else {
            emailService.sendPortalLinksEmail(userEmail, portalUrls);
        }

        return ApiResponse.success(genericMessage);
    }
}
