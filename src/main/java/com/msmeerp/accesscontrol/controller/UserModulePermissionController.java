package com.msmeerp.accesscontrol.controller;

import com.msmeerp.accesscontrol.dto.AssignModulePermissionRequest;
import com.msmeerp.accesscontrol.dto.ModulePermissionDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.service.UserModulePermissionService;
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
@RequestMapping("/users/{userId}/permissions")
@RequiredArgsConstructor
public class UserModulePermissionController {

    private final UserModulePermissionService userModulePermissionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_MANAGE')")
    public ResponseEntity<ApiResponse<List<ModulePermissionDto>>> getPermissions(@PathVariable Long userId) {
        List<ModulePermissionDto> response = userModulePermissionService.getPermissionsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{moduleCode}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_MANAGE')")
    public ResponseEntity<ApiResponse<ModulePermissionDto>> assignPermission(
            @PathVariable Long userId,
            @PathVariable ModuleCode moduleCode,
            @Valid @RequestBody AssignModulePermissionRequest request
    ) {
        ModulePermissionDto response = userModulePermissionService.assignModulePermission(userId, moduleCode, request.getActions());
        return ResponseEntity.ok(ApiResponse.success(response, "Module permission updated successfully"));
    }

    @DeleteMapping("/{moduleCode}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> revokePermission(@PathVariable Long userId, @PathVariable ModuleCode moduleCode) {
        userModulePermissionService.revokeModulePermission(userId, moduleCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Module permission revoked successfully"));
    }
}
