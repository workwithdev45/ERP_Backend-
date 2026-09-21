package com.hms.auth.service.impl;

import com.hms.accesscontrol.entity.Permission;
import com.hms.accesscontrol.entity.Role;
import com.hms.auth.dto.LoginRequest;
import com.hms.auth.dto.LoginResponse;
import com.hms.auth.dto.RefreshTokenRequest;
import com.hms.auth.dto.UserProfileResponse;
import com.hms.auth.security.JwtTokenProvider;
import com.hms.auth.security.UserPrincipal;
import com.hms.auth.service.AuthService;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.exception.UnauthorizedException;
import com.hms.common.util.SecurityUtils;
import com.hms.tenant.context.TenantContext;
import com.hms.tenant.service.TenantResolverService;
import com.hms.user.entity.User;
import com.hms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final TenantResolverService tenantResolverService;

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

        Set<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

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

        Set<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

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
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getBranchName() : null)
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

        UserPrincipal userPrincipal = UserPrincipal.create(user);
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
                .permissions(user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}
