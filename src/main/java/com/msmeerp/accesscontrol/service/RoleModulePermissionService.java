package com.msmeerp.accesscontrol.service;

import com.msmeerp.accesscontrol.dto.ModulePermissionDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.PermissionAction;

import java.util.List;
import java.util.Set;

public interface RoleModulePermissionService {

    List<ModulePermissionDto> getPermissionsForRole(Long roleId);

    ModulePermissionDto assignModulePermission(Long roleId, ModuleCode moduleCode, Set<PermissionAction> actions);

    void revokeModulePermission(Long roleId, ModuleCode moduleCode);
}
