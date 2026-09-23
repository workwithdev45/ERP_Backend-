package com.msmeerp.user.service.impl;

import com.msmeerp.accesscontrol.entity.Role;
import com.msmeerp.accesscontrol.repository.RoleRepository;
import com.msmeerp.auth.security.JwtTokenProvider;
import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.exception.UnauthorizedException;
import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.common.service.EmailService;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.dto.TenantResponse;
import com.msmeerp.tenant.entity.UserTenantMap;
import com.msmeerp.tenant.repository.UserTenantMapRepository;
import com.msmeerp.tenant.service.TenantResolverService;
import com.msmeerp.user.dto.AcceptUserInviteRequest;
import com.msmeerp.user.dto.UserCreateRequest;
import com.msmeerp.user.dto.UserInviteRequest;
import com.msmeerp.user.dto.UserResponse;
import com.msmeerp.user.dto.UserUpdateRequest;
import com.msmeerp.user.entity.User;
import com.msmeerp.user.repository.UserRepository;
import com.msmeerp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserTenantMapRepository userTenantMapRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantResolverService tenantResolverService;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    @Value("${app.portal.base-domain:msmeerp.com}")
    private String baseDomain;

    @Value("${app.portal.scheme:http}")
    private String scheme;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant ID is required to create a user");
        }

        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new BadRequestException("At least one role ID is required to create a user");
        }

        if (userRepository.existsByTenantIdAndUsername(tenantId, request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already in use");
        }

        if (userRepository.existsByTenantIdAndEmail(tenantId, request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already in use");
        }

        Set<Role> roles = resolveRoles(request.getRoleIds());
        if (roles.isEmpty()) {
            throw new BadRequestException("The specified role IDs were not found for tenant: " + tenantId);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(User.UserStatus.ACTIVE)
                .roles(roles)
                .build();
        user.setTenantId(tenantId);

        User saved = userRepository.save(user);
        updateUserTenantMap(saved.getEmail(), tenantResolverService.getTenantById(tenantId).getPortalId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<User> page = userRepository.findByTenantId(tenantId, pageable);
        return PagedResponse.from(page.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getTenantId() != null && !user.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("User", "id", id);
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = resolveRoles(request.getRoleIds());
            user.setRoles(roles);
        }

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getTenantId() != null && !user.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("User", "id", id);
        }

        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse inviteUser(UserInviteRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant ID is required to invite a user");
        }

        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new BadRequestException("Email '" + email + "' is already in use");
        }

        Set<Role> roles = resolveRoles(request.getRoleIds());
        if (roles.isEmpty()) {
            throw new BadRequestException("The specified role IDs were not found for tenant: " + tenantId);
        }

        String username = uniqueUsernameFromEmail(tenantId, email);
        String placeholderPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .username(username)
                .email(email)
                .password(placeholderPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(User.UserStatus.PENDING_VERIFICATION)
                .roles(roles)
                .build();
        user.setTenantId(tenantId);

        User saved = userRepository.save(user);
        TenantResponse tenant = tenantResolverService.getTenantById(tenantId);
        updateUserTenantMap(saved.getEmail(), tenant.getPortalId());

        String inviteToken = tokenProvider.generateUserInviteToken(email, tenantId);
        String inviteUrl = "%s://%s.%s/accept-invite?portalId=%s&email=%s&token=%s"
                .formatted(scheme, tenant.getPortalId(), baseDomain, tenant.getPortalId(), email, inviteToken);
        emailService.sendUserInviteEmail(email, tenant.getName(), inviteUrl);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ApiResponse<Void> acceptInvite(AcceptUserInviteRequest request) {
        String tenantId = TenantContext.getTenantId();
        String email = request.getEmail().toLowerCase().trim();

        if (tenantId == null || !tokenProvider.validateUserInviteToken(request.getInviteToken(), email, tenantId)) {
            throw new UnauthorizedException("Invite link is invalid or has expired");
        }

        User user = userRepository.findByTenantIdAndEmail(tenantId, email)
                .orElseThrow(() -> new BadRequestException("No pending invite found for this email"));

        if (user.getStatus() != User.UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("This invite has already been accepted");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        return ApiResponse.success(null, "Invitation accepted. You can now sign in.");
    }

    private String uniqueUsernameFromEmail(String tenantId, String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByTenantIdAndUsername(tenantId, candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private void updateUserTenantMap(String email, String portalId) {
        UserTenantMap map = userTenantMapRepository.findByEmail(email)
                .orElse(UserTenantMap.builder()
                        .email(email)
                        .tenantIds(portalId)
                        .build());

        if (map.getId() != null) {
            Set<String> set = new HashSet<>(Arrays.asList(map.getTenantIds().split(",")));
            set.add(portalId);
            map.setTenantIds(String.join(",", set));
        }
        userTenantMapRepository.save(map);
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        String tenantId = TenantContext.getTenantId();
        return roleRepository.findAllById(roleIds).stream()
                .filter(r -> tenantId != null && tenantId.equals(r.getTenantId()))
                .collect(Collectors.toSet());
    }

    private UserResponse mapToResponse(User user) {
        String tenantName = tenantResolverService.getTenantName(user.getTenantId());
        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .tenantName(tenantName)
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
