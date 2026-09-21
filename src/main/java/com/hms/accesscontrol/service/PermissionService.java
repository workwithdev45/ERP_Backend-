package com.hms.accesscontrol.service;

import com.hms.accesscontrol.dto.PermissionDto;

import java.util.List;

public interface PermissionService {
    PermissionDto createPermission(PermissionDto permissionDto);
    List<PermissionDto> getAllPermissions();
    List<PermissionDto> getPermissionsByModule(String module);
}
