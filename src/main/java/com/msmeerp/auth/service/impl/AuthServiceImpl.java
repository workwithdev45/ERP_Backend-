package com.msmeerp.auth.service.impl;

import com.msmeerp.accesscontrol.entity.Permission;
import com.msmeerp.accesscontrol.entity.Role;
import com.msmeerp.accesscontrol.repository.UserModulePermissionRepository;
import com.msmeerp.auth.dto.ForgotPasswordRequest;
import com.msmeerp.auth.dto.LoginRequest;
import com.msmeerp.auth.dto.LoginResponse;
import com.msmeerp.auth.dto.RefreshTokenRequest;
import com.msmeerp.auth.dto.ResetPasswordRequest;
import com.msmeerp.auth.dto.UserProfileResponse;
import com.msmeerp.auth.security.JwtTokenProvider;
import com.msmeerp.auth.security.UserPrincipal;
import com.msmeerp.auth.service.AuthService;
import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.exception.UnauthorizedException;
import com.msmeerp.common.service.EmailService;
import com.msmeerp.common.util.AppConstants;
import com.msmeerp.common.util.SecurityUtils;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.tenant.service.TenantResolverService;
import com.msmeerp.user.entity.User;
import com.msmeerp.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_RESET_LINK = "This reset link is invalid or has expired. Please request a new one.";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final TenantResolverService tenantResolverService;
    private final UserModulePermissionRepository userModulePermissionRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.portal.base-domain:msmeerp.com}")
    private String baseDomain;

    @Value("${app.portal.scheme:https}")
    private String scheme;

    /** Optional override for where emailed links point (e.g. http://localhost:5173 in local dev). */
    @Value("${app.frontend.url:}")
    private String frontendUrl;

    private Set<String> effectivePermissionNames(User user) {
        Set<String> rolePermissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        Set<String> modulePermissions = userModulePermissionRepository
                .findByTenantIdAndUserId(user.getTenantId(), user.getId()).stream()
                .flatMap(mp -> mp.getActions().stream()
                        .map(action -> mp.getModuleCode().name() + "_" + action.name()))
                .collect(Collectors.toSet());

        rolePermissions.addAll(modulePermissions);
        return rolePermissions;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getUsername(), userPrincipal.getTenantId());

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = effectivePermissionNames(user);

        String tenantName = tenantResolverService.getTenantName(userPrincipal.getTenantId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .tenantId(userPrincipal.getTenantId())
                .tenantName(tenantName)
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
        String tenantId = TenantContext.getTenantId();

        User user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseGet(() -> userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username)));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = effectivePermissionNames(user);

        String tenantName = tenantResolverService.getTenantName(user.getTenantId());

        return UserProfileResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .tenantName(tenantName)
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus().name())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String username = tokenProvider.getUsernameFromJwt(request.getRefreshToken());
        String tenantId = tokenProvider.getTenantIdFromJwt(request.getRefreshToken());

        User user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        var modulePermissions = userModulePermissionRepository.findByTenantIdAndUserId(tenantId, user.getId());
        UserPrincipal userPrincipal = UserPrincipal.create(user, modulePermissions);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(username, tenantId);
        String tenantName = tenantResolverService.getTenantName(tenantId);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .tenantId(tenantId)
                .tenantName(tenantName)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .permissions(effectivePermissionNames(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String tenantId = TenantContext.getTenantId();
        String email = request.getEmail().toLowerCase().trim();

        if (tenantId == null || AppConstants.DEFAULT_TENANT_ID.equals(tenantId)) {
            log.info("Password reset requested without a workspace; ignoring");
            return;
        }

        Optional<Tenant> tenant = tenantRepository.findById(tenantId).filter(Tenant::isActive);
        Optional<User> user = userRepository.findByTenantIdAndEmail(tenantId, email)
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE);
        if (tenant.isEmpty() || user.isEmpty()) {
            // Same response either way so the endpoint can't be used to discover accounts.
            log.info("Password reset requested for an unknown or inactive account in workspace {}", tenantId);
            return;
        }

        String token = tokenProvider.generatePasswordResetToken(email, tenantId, user.get().getPassword());
        String resetUrl = buildResetUrl(tenant.get().getPortalId(), token);
        String companyName = StringUtils.hasText(tenant.get().getDisplayName())
                ? tenant.get().getDisplayName()
                : tenant.get().getName();
        long validMinutes = tokenProvider.getPasswordResetTokenExpirationMs() / 60_000;

        emailService.sendPasswordResetEmail(email, companyName, resetUrl, validMinutes);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Claims claims = tokenProvider.parsePasswordResetToken(request.getToken());
        if (claims == null) {
            throw new BadRequestException(INVALID_RESET_LINK);
        }

        String tenantId = claims.get("tenantId", String.class);
        User user = userRepository.findByTenantIdAndEmail(tenantId, claims.getSubject())
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_LINK));

        // The token is bound to the password it was issued for, so a used link can't be replayed.
        if (!tokenProvider.passwordResetTokenMatches(claims, user.getPassword())) {
            throw new BadRequestException(INVALID_RESET_LINK);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset completed for user {} in workspace {}", user.getId(), tenantId);
    }

    private String buildResetUrl(String portalId, String token) {
        String base = StringUtils.hasText(frontendUrl)
                ? frontendUrl.replaceAll("/+$", "")
                : "%s://%s.%s".formatted(scheme, portalId, baseDomain);
        return base + "/reset-password?workspace=" + URLEncoder.encode(portalId, StandardCharsets.UTF_8)
                + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
