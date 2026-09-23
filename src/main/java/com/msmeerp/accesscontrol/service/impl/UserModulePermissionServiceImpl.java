package com.msmeerp.accesscontrol.service.impl;

import com.msmeerp.accesscontrol.dto.ModulePermissionDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.PermissionAction;
import com.msmeerp.accesscontrol.entity.UserModulePermission;
import com.msmeerp.accesscontrol.repository.UserModulePermissionRepository;
import com.msmeerp.accesscontrol.service.UserModulePermissionService;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.user.entity.User;
import com.msmeerp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserModulePermissionServiceImpl implements UserModulePermissionService {

    private final UserModulePermissionRepository userModulePermissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModulePermissionDto> getPermissionsForUser(Long userId) {
        String tenantId = TenantContext.getTenantId();
        requireUserInTenant(userId, tenantId);

        return userModulePermissionRepository.findByTenantIdAndUserId(tenantId, userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public ModulePermissionDto assignModulePermission(Long userId, ModuleCode moduleCode, Set<PermissionAction> actions) {
        String tenantId = TenantContext.getTenantId();
        requireUserInTenant(userId, tenantId);

        UserModulePermission permission = userModulePermissionRepository
                .findByTenantIdAndUserIdAndModuleCode(tenantId, userId, moduleCode)
                .orElseGet(() -> {
                    UserModulePermission created = UserModulePermission.builder()
                            .userId(userId)
                            .moduleCode(moduleCode)
                            .build();
                    created.setTenantId(tenantId);
                    return created;
                });

        permission.setActions(actions);
        UserModulePermission saved = userModulePermissionRepository.save(permission);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void revokeModulePermission(Long userId, ModuleCode moduleCode) {
        String tenantId = TenantContext.getTenantId();
        requireUserInTenant(userId, tenantId);
        userModulePermissionRepository.deleteByTenantIdAndUserIdAndModuleCode(tenantId, userId, moduleCode);
    }

    private void requireUserInTenant(Long userId, String tenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (tenantId == null || !tenantId.equals(user.getTenantId())) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
    }

    private ModulePermissionDto mapToDto(UserModulePermission permission) {
        return ModulePermissionDto.builder()
                .moduleCode(permission.getModuleCode())
                .actions(permission.getActions())
                .build();
    }
}
