package com.hms.hospital.service.impl;

import com.hms.auth.security.JwtTokenProvider;
import com.hms.common.exception.BadRequestException;
import com.hms.common.response.ApiResponse;
import com.hms.common.service.EmailService;
import com.hms.hospital.dto.CheckPortalIdResponse;
import com.hms.hospital.dto.FindHospitalRequest;
import com.hms.hospital.dto.HospitalRegisterInitRequest;
import com.hms.hospital.dto.ReservePortalRequest;
import com.hms.hospital.dto.SetAdminPasswordRequest;
import com.hms.hospital.dto.SetAdminPasswordResponse;
import com.hms.hospital.dto.VerifyOtpRequest;
import com.hms.hospital.dto.VerifyOtpResponse;
import com.hms.hospital.entity.HospitalOnboarding;
import com.hms.hospital.entity.StaffHospitalMap;
import com.hms.hospital.repository.HospitalOnboardingRepository;
import com.hms.hospital.repository.StaffHospitalMapRepository;
import com.hms.hospital.service.HospitalOnboardingService;
import com.hms.hospital.service.HospitalProvisioningService;
import com.hms.tenant.entity.Tenant;
import com.hms.tenant.repository.TenantRepository;
import com.hms.user.entity.User;
import com.hms.user.repository.UserRepository;
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
public class HospitalOnboardingServiceImpl implements HospitalOnboardingService {

    private final HospitalOnboardingRepository onboardingRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StaffHospitalMapRepository staffHospitalMapRepository;
    private final HospitalProvisioningService provisioningService;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.portal.base-domain:medicorehms.in}")
    private String baseDomain;

    @Value("${app.portal.scheme:https}")
    private String scheme;

    private static final Set<String> RESERVED_WORDS = Set.of(
            "admin", "administrator", "api", "app", "auth", "billing", "cdn",
            "dashboard", "dev", "dns", "doc", "docs", "ftp", "help", "hms",
            "host", "mail", "medicore", "medicorehms", "portal", "root",
            "secure", "server", "signup", "ssl", "stage", "staging", "status",
            "superadmin", "support", "sysadmin", "system", "test", "web",
            "webmail", "www"
    );

    private static final Pattern PORTAL_ID_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ApiResponse<Void> register(HospitalRegisterInitRequest request) {
        String adminEmail = request.getAdminEmail().toLowerCase().trim();

        // 1. Check if this email already owns an activated hospital tenant
        Optional<Tenant> existingTenant = tenantRepository.findByAdminEmail(adminEmail);
        if (existingTenant.isPresent() && existingTenant.get().isActive()) {
            log.info("Registration rejected: admin {} already owns active hospital {}", adminEmail, existingTenant.get().getPortalId());
            throw new BadRequestException("A hospital portal '" + existingTenant.get().getPortalId() + "' is already registered with this email. Please sign in instead.");
        }

        // 2. Generate secure 4-digit OTP
        int randomCode = 1000 + secureRandom.nextInt(9000);
        String otp = String.valueOf(randomCode);
        long otpValidUntil = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes TTL

        // 3. Upsert HospitalOnboarding Record
        HospitalOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
                .orElse(HospitalOnboarding.builder()
                        .adminEmail(adminEmail)
                        .build());

        onboarding.setAdminPhone(request.getAdminPhone());
        onboarding.setOtp(otp);
        onboarding.setOtpValidUntil(otpValidUntil);
        onboarding.setOtpAttempts(0);
        onboarding.setLastOtpSentAt(Instant.now());
        onboarding.setStatus(HospitalOnboarding.OnboardingStatus.PENDING);
        onboardingRepository.save(onboarding);

        // 4. Send OTP email via SMTP
        emailService.sendOtpEmail(adminEmail, otp);

        log.info("OTP generated and email sent to: {}", adminEmail);

        return ApiResponse.success("Verification code sent to your email. Please verify the OTP to continue registration.");
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String adminEmail = request.getAdminEmail().toLowerCase().trim();

        // 1. Find pending onboarding record
        HospitalOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
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

        onboarding.setStatus(HospitalOnboarding.OnboardingStatus.EMAIL_VERIFIED);
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
                    .message("Portal ID is already taken by another hospital")
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
        HospitalOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Registration session invalid or expired. Please verify your email again."));

        if (onboarding.getStatus() == HospitalOnboarding.OnboardingStatus.PENDING) {
            throw new BadRequestException("Please verify your email OTP before reserving a portal.");
        }

        // 3. Race condition protection: re-validate portal ID
        CheckPortalIdResponse check = checkPortalId(portalId);
        if (!check.isAvailable()) {
            throw new BadRequestException(check.getMessage());
        }

        // 4. Provision dedicated hospital tenant
        provisioningService.provisionHospital(adminEmail, portalId, request.isStartBlank());

        // 5. Update onboarding record
        onboarding.setPortalId(portalId);
        onboarding.setStatus(HospitalOnboarding.OnboardingStatus.PORTAL_RESERVED);
        onboardingRepository.save(onboarding);

        return ApiResponse.success("Hospital portal reserved and provisioned successfully!");
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
        HospitalOnboarding onboarding = onboardingRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Registration session invalid or expired. Please verify your email again."));

        if (onboarding.getStatus() != HospitalOnboarding.OnboardingStatus.PORTAL_RESERVED
                || !portalId.equalsIgnoreCase(onboarding.getPortalId())) {
            throw new BadRequestException("Portal must be reserved before setting admin password.");
        }

        // 3. Find Super Admin user for this tenant and update password
        Tenant tenant = tenantRepository.findByPortalId(portalId)
                .or(() -> tenantRepository.findById(portalId))
                .orElseThrow(() -> new BadRequestException("Hospital portal not found: " + portalId));

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
        onboarding.setStatus(HospitalOnboarding.OnboardingStatus.ACTIVE);
        onboardingRepository.save(onboarding);

        // 5. Construct live portal URL
        String portalUrl = "%s://%s.%s".formatted(scheme, portalId, baseDomain);

        // 6. Send welcome email via SMTP
        emailService.sendWelcomeEmail(adminEmail, portalId, portalUrl);

        return SetAdminPasswordResponse.builder()
                .error(false)
                .message("Admin password set and hospital is ready!")
                .portalUrl(portalUrl)
                .build();
    }

    @Override
    public ApiResponse<Void> findHospitals(FindHospitalRequest request) {
        String userEmail = request.getUserEmail().toLowerCase().trim();

        // Look up portal IDs from StaffHospitalMap or Tenants
        Optional<StaffHospitalMap> mapOpt = staffHospitalMapRepository.findByEmail(userEmail);
        List<String> portalIds = new ArrayList<>();

        if (mapOpt.isPresent() && StringUtils.hasText(mapOpt.get().getHospitalIds())) {
            portalIds.addAll(Arrays.asList(mapOpt.get().getHospitalIds().split(",")));
        } else {
            Optional<Tenant> tenantOpt = tenantRepository.findByAdminEmail(userEmail);
            tenantOpt.ifPresent(t -> portalIds.add(t.getPortalId()));
        }

        if (portalIds.isEmpty()) {
            throw new BadRequestException("This email is not linked to any hospital portal.");
        }

        List<String> portalUrls = portalIds.stream()
                .map(id -> "%s://%s.%s".formatted(scheme, id.trim(), baseDomain))
                .toList();

        // Send links via SMTP email
        emailService.sendHospitalLinksEmail(userEmail, portalUrls);

        return ApiResponse.success("Check your email for your hospital portal link(s).");
    }
}
