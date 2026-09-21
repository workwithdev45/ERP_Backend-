package com.hms.user.service.impl;

import com.hms.accesscontrol.entity.Role;
import com.hms.accesscontrol.repository.RoleRepository;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.response.PagedResponse;
import com.hms.hospital.dto.BranchAdminCreateRequest;
import com.hms.hospital.entity.Branch;
import com.hms.hospital.entity.StaffHospitalMap;
import com.hms.hospital.repository.BranchRepository;
import com.hms.hospital.repository.StaffHospitalMapRepository;
import com.hms.tenant.context.TenantContext;
import com.hms.tenant.service.TenantResolverService;
import com.hms.user.dto.UserCreateRequest;
import com.hms.user.dto.UserResponse;
import com.hms.user.dto.UserUpdateRequest;
import com.hms.user.entity.User;
import com.hms.user.repository.UserRepository;
import com.hms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final StaffHospitalMapRepository staffHospitalMapRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantResolverService tenantResolverService;

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
        updateStaffHospitalMap(saved.getEmail(), tenantId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse createBranchAdmin(BranchAdminCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant ID is required to create a branch admin");
        }

        Branch branch;
        if (request.getBranchId() != null && !request.getBranchId().isBlank()) {
            branch = branchRepository.findByTenantIdAndId(tenantId, request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getBranchId()));
        } else if (request.getBranchName() != null && !request.getBranchName().isBlank()) {
            branch = branchRepository.findByTenantIdAndName(tenantId, request.getBranchName().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "name", request.getBranchName()));
        } else {
            throw new BadRequestException("Branch ID or Branch Name is required in request body to assign branch admin");
        }

        // Validate if Branch already has an active Admin
        boolean adminExists = userRepository.existsAdminByTenantIdAndBranchId(
                tenantId, branch.getId(), "ADMIN", User.UserStatus.ACTIVE
        );
        if (adminExists) {
            Optional<User> existingAdmin = userRepository.findAdminByTenantIdAndBranchId(
                    tenantId, branch.getId(), "ADMIN", User.UserStatus.ACTIVE
            );
            String adminInfo = existingAdmin
                    .map(u -> u.getFirstName() + " " + u.getLastName() + " (" + u.getEmail() + ")")
                    .orElse("an existing user");
            throw new BadRequestException("Branch '" + branch.getBranchName() + "' already has an assigned Admin: " + adminInfo);
        }

        // Check username uniqueness
        if (userRepository.existsByTenantIdAndUsername(tenantId, request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already in use");
        }

        // Check email uniqueness
        if (userRepository.existsByTenantIdAndEmail(tenantId, request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already in use");
        }

        // Fetch ADMIN role for this tenant
        Role adminRole = roleRepository.findByTenantIdAndName(tenantId, "ADMIN")
                .or(() -> roleRepository.findByTenantIdAndName(tenantId, "BRANCH_ADMIN"))
                .orElseThrow(() -> new BadRequestException("ADMIN role not found for tenant: " + tenantId));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(User.UserStatus.ACTIVE)
                .branch(branch)
                .roles(Set.of(adminRole))
                .build();
        user.setTenantId(tenantId);

        User savedUser = userRepository.save(user);
        updateStaffHospitalMap(savedUser.getEmail(), tenantId);

        return mapToResponse(savedUser);
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

    private void updateStaffHospitalMap(String email, String portalId) {
        StaffHospitalMap map = staffHospitalMapRepository.findByEmail(email)
                .orElse(StaffHospitalMap.builder()
                        .email(email)
                        .hospitalIds(portalId)
                        .build());

        if (map.getId() != null) {
            Set<String> set = new HashSet<>(Arrays.asList(map.getHospitalIds().split(",")));
            set.add(portalId);
            map.setHospitalIds(String.join(",", set));
        }
        staffHospitalMapRepository.save(map);
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
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getBranchName() : null)
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
