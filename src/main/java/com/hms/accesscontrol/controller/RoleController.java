package com.hms.accesscontrol.controller;

import com.hms.accesscontrol.dto.RoleDto;
import com.hms.accesscontrol.service.RoleService;
import com.hms.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody RoleDto roleDto) {
        RoleDto response = roleService.createRole(roleDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Role created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable Long id) {
        RoleDto response = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        List<RoleDto> response = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto roleDto) {
        RoleDto response = roleService.updateRole(id, roleDto);
        return ResponseEntity.ok(ApiResponse.success(response, "Role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }
}
