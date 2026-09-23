package com.msmeerp.accesscontrol.service;

import com.msmeerp.accesscontrol.dto.ModulePermissionDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.PermissionAction;

import java.util.List;
import java.util.Set;

public interface UserModulePermissionService {

    List<ModulePermissionDto> getPermissionsForUser(Long userId);

    ModulePermissionDto assignModulePermission(Long userId, ModuleCode moduleCode, Set<PermissionAction> actions);

    void revokeModulePermission(Long userId, ModuleCode moduleCode);
}
