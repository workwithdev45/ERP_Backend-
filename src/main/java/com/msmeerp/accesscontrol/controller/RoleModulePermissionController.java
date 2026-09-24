package com.msmeerp.accesscontrol.controller;

import com.msmeerp.accesscontrol.dto.AssignModulePermissionRequest;
import com.msmeerp.accesscontrol.dto.ModulePermissionDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.service.RoleModulePermissionService;
import com.msmeerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles/{roleId}/permissions")
@RequiredArgsConstructor
public class RoleModulePermissionController {

    private final RoleModulePermissionService roleModulePermissionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<ModulePermissionDto>>> getPermissions(@PathVariable Long roleId) {
        List<ModulePermissionDto> response = roleModulePermissionService.getPermissionsForRole(roleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{moduleCode}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<ModulePermissionDto>> assignPermission(
            @PathVariable Long roleId,
            @PathVariable ModuleCode moduleCode,
            @Valid @RequestBody AssignModulePermissionRequest request
    ) {
        ModulePermissionDto response = roleModulePermissionService.assignModulePermission(roleId, moduleCode, request.getActions());
        return ResponseEntity.ok(ApiResponse.success(response, "Module permission updated successfully"));
    }

    @DeleteMapping("/{moduleCode}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> revokePermission(@PathVariable Long roleId, @PathVariable ModuleCode moduleCode) {
        roleModulePermissionService.revokeModulePermission(roleId, moduleCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Module permission revoked successfully"));
    }
}
