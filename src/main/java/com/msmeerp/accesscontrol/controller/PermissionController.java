package com.msmeerp.accesscontrol.controller;

import com.msmeerp.accesscontrol.dto.PermissionDto;
import com.msmeerp.accesscontrol.service.PermissionService;
import com.msmeerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // Platform-reserved: the permission catalog is seeded centrally and not editable by tenant Admins.
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<PermissionDto>> createPermission(@Valid @RequestBody PermissionDto permissionDto) {
        PermissionDto response = permissionService.createPermission(permissionDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Permission created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        List<PermissionDto> response = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/module/{module}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getPermissionsByModule(@PathVariable String module) {
        List<PermissionDto> response = permissionService.getPermissionsByModule(module);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
