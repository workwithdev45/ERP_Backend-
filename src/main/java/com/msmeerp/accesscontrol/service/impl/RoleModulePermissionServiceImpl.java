package com.msmeerp.accesscontrol.service.impl;

import com.msmeerp.accesscontrol.dto.ModulePermissionDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.PermissionAction;
import com.msmeerp.accesscontrol.entity.Role;
import com.msmeerp.accesscontrol.entity.RoleModulePermission;
import com.msmeerp.accesscontrol.repository.RoleModulePermissionRepository;
import com.msmeerp.accesscontrol.repository.RoleRepository;
import com.msmeerp.accesscontrol.service.RoleModulePermissionService;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleModulePermissionServiceImpl implements RoleModulePermissionService {

    private final RoleModulePermissionRepository roleModulePermissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModulePermissionDto> getPermissionsForRole(Long roleId) {
        String tenantId = TenantContext.getTenantId();
        requireRoleInTenant(roleId, tenantId);

        return roleModulePermissionRepository.findByTenantIdAndRoleId(tenantId, roleId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public ModulePermissionDto assignModulePermission(Long roleId, ModuleCode moduleCode, Set<PermissionAction> actions) {
        String tenantId = TenantContext.getTenantId();
        requireRoleInTenant(roleId, tenantId);

        RoleModulePermission permission = roleModulePermissionRepository
                .findByTenantIdAndRoleIdAndModuleCode(tenantId, roleId, moduleCode)
                .orElseGet(() -> {
                    RoleModulePermission created = RoleModulePermission.builder()
                            .roleId(roleId)
                            .moduleCode(moduleCode)
                            .build();
                    created.setTenantId(tenantId);
                    return created;
                });

        permission.setActions(actions);
        RoleModulePermission saved = roleModulePermissionRepository.save(permission);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void revokeModulePermission(Long roleId, ModuleCode moduleCode) {
        String tenantId = TenantContext.getTenantId();
        requireRoleInTenant(roleId, tenantId);
        roleModulePermissionRepository.deleteByTenantIdAndRoleIdAndModuleCode(tenantId, roleId, moduleCode);
    }

    private void requireRoleInTenant(Long roleId, String tenantId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        if (tenantId == null || !tenantId.equals(role.getTenantId())) {
            throw new ResourceNotFoundException("Role", "id", roleId);
        }
    }

    private ModulePermissionDto mapToDto(RoleModulePermission permission) {
        return ModulePermissionDto.builder()
                .moduleCode(permission.getModuleCode())
                .actions(permission.getActions())
                .build();
    }
}
