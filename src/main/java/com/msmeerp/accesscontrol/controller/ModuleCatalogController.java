package com.msmeerp.accesscontrol.controller;

import com.msmeerp.accesscontrol.dto.TenantModuleDto;
import com.msmeerp.accesscontrol.dto.UpdateTenantModuleRequest;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.PermissionAction;
import com.msmeerp.accesscontrol.service.TenantModuleService;
import com.msmeerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
public class ModuleCatalogController {

    private final TenantModuleService tenantModuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModuleCode>>> listModules() {
        return ResponseEntity.ok(ApiResponse.success(List.of(ModuleCode.values())));
    }

    @GetMapping("/actions")
    public ResponseEntity<ApiResponse<List<PermissionAction>>> listActions() {
        return ResponseEntity.ok(ApiResponse.success(List.of(PermissionAction.values())));
    }

    /** G14: which modules are switched on for the caller's tenant — drives sidebar visibility. */
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<TenantModuleDto>>> listEnabledModules() {
        return ResponseEntity.ok(ApiResponse.success(tenantModuleService.getModulesForCurrentTenant()));
    }

    @PutMapping("/{moduleCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantModuleDto>> setModuleEnabled(
            @PathVariable ModuleCode moduleCode, @Valid @RequestBody UpdateTenantModuleRequest request) {
        TenantModuleDto response = tenantModuleService.setModuleEnabled(moduleCode, request.isEnabled());
        return ResponseEntity.ok(ApiResponse.success(response, "Module updated"));
    }
}
