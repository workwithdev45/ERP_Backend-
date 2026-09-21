package com.hms.accesscontrol.service.impl;

import com.hms.accesscontrol.dto.RoleDto;
import com.hms.accesscontrol.entity.Permission;
import com.hms.accesscontrol.entity.Role;
import com.hms.accesscontrol.repository.PermissionRepository;
import com.hms.accesscontrol.repository.RoleRepository;
import com.hms.accesscontrol.service.RoleService;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public RoleDto createRole(RoleDto roleDto) {
        String tenantId = TenantContext.getTenantId();
        if (roleRepository.existsByTenantIdAndName(tenantId, roleDto.getName())) {
            throw new BadRequestException("Role '" + roleDto.getName() + "' already exists for this tenant");
        }

        Set<Permission> permissions = resolvePermissions(roleDto.getPermissionIds());

        Role role = Role.builder()
                .name(roleDto.getName())
                .description(roleDto.getDescription())
                .systemRole(roleDto.isSystemRole())
                .permissions(permissions)
                .build();
        role.setTenantId(tenantId);

        Role saved = roleRepository.save(role);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return mapToDto(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        String tenantId = TenantContext.getTenantId();
        return roleRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public RoleDto updateRole(Long id, RoleDto roleDto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        // Safeguard: Do not allow renaming the core ADMIN role
        if ("ADMIN".equalsIgnoreCase(role.getName()) && !role.getName().equalsIgnoreCase(roleDto.getName())) {
            throw new BadRequestException("The core ADMIN role name cannot be modified");
        }

        role.setName(roleDto.getName());
        if (roleDto.getDescription() != null) {
            role.setDescription(roleDto.getDescription());
        }

        // Dynamically update checkboxes / permissions for this role (e.g. DOCTOR, NURSE, RECEPTIONIST)
        if (roleDto.getPermissionIds() != null) {
            role.setPermissions(resolvePermissions(roleDto.getPermissionIds()));
        }

        Role updated = roleRepository.save(role);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        // Safeguard: Do not allow deleting the core ADMIN role
        if ("ADMIN".equalsIgnoreCase(role.getName())) {
            throw new BadRequestException("The core ADMIN role cannot be deleted");
        }

        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(permissionRepository.findAllById(ids));
    }

    private RoleDto mapToDto(Role role) {
        Set<String> permNames = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
        Set<Long> permIds = role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .systemRole(role.isSystemRole())
                .permissionIds(permIds)
                .permissionNames(permNames)
                .build();
    }
}
