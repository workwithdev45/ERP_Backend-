package com.msmeerp.accesscontrol.controller;

import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.PermissionAction;
import com.msmeerp.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
public class ModuleCatalogController {

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModuleCode>>> listModules() {
        return ResponseEntity.ok(ApiResponse.success(List.of(ModuleCode.values())));
    }

    @GetMapping("/actions")
    public ResponseEntity<ApiResponse<List<PermissionAction>>> listActions() {
        return ResponseEntity.ok(ApiResponse.success(List.of(PermissionAction.values())));
    }
}
