package com.msmeerp.tenant.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.tenant.dto.CompanyDetailsResponse;
import com.msmeerp.tenant.dto.CompanyDetailsUpdateRequest;
import com.msmeerp.tenant.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<ApiResponse<CompanyDetailsResponse>> getCompanyDetails() {
        return ResponseEntity.ok(ApiResponse.success(companyService.getCompanyDetails()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CompanyDetailsResponse>> updateCompanyDetails(
            @Valid @RequestBody CompanyDetailsUpdateRequest request) {
        CompanyDetailsResponse response = companyService.updateCompanyDetails(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Company details updated"));
    }
}
