package com.hms.hospital.controller;

import com.hms.common.response.ApiResponse;
import com.hms.hospital.dto.BranchAdminCreateRequest;
import com.hms.hospital.dto.BranchCreateRequest;
import com.hms.hospital.dto.BranchResponse;
import com.hms.hospital.dto.DepartmentDto;
import com.hms.hospital.service.HospitalStructureService;
import com.hms.user.dto.UserResponse;
import com.hms.user.service.UserService;
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
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final HospitalStructureService hospitalStructureService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @Valid @RequestBody BranchCreateRequest request
    ) {
        BranchResponse response = hospitalStructureService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Branch created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR', 'DOCTOR', 'NURSE', 'RECEPTIONIST') or hasAuthority('SETTINGS_MANAGE') or hasAuthority('PATIENT_READ')")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranches() {
        List<BranchResponse> response = hospitalStructureService.getAllBranchResponses();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR', 'DOCTOR', 'NURSE', 'RECEPTIONIST') or hasAuthority('SETTINGS_MANAGE') or hasAuthority('PATIENT_READ')")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable String id) {
        BranchResponse response = hospitalStructureService.getBranchResponseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USER_MANAGE')")
    public ResponseEntity<ApiResponse<UserResponse>> createBranchAdminWithBody(
            @Valid @RequestBody BranchAdminCreateRequest request
    ) {
        UserResponse response = userService.createBranchAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Branch Admin created and assigned successfully"));
    }

    @PostMapping("/{branchId}/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USER_MANAGE')")
    public ResponseEntity<ApiResponse<UserResponse>> createBranchAdmin(
            @PathVariable String branchId,
            @Valid @RequestBody BranchAdminCreateRequest request
    ) {
        request.setBranchId(branchId);
        UserResponse response = userService.createBranchAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Branch Admin created and assigned successfully"));
    }

    @GetMapping("/{branchId}/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR', 'DOCTOR', 'NURSE', 'RECEPTIONIST') or hasAuthority('SETTINGS_MANAGE') or hasAuthority('PATIENT_READ')")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getDepartmentsByBranch(@PathVariable String branchId) {
        List<DepartmentDto> response = hospitalStructureService.getDepartmentsByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
