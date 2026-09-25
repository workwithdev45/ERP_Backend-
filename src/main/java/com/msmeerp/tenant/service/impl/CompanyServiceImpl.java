package com.msmeerp.tenant.service.impl;

import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.dto.CompanyDetailsResponse;
import com.msmeerp.tenant.dto.CompanyDetailsUpdateRequest;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.tenant.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public CompanyDetailsResponse getCompanyDetails() {
        return mapToResponse(currentTenant());
    }

    @Override
    @Transactional
    public CompanyDetailsResponse updateCompanyDetails(CompanyDetailsUpdateRequest request) {
        Tenant tenant = currentTenant();
        tenant.setLegalName(request.getLegalName());
        tenant.setGstin(request.getGstin() == null || request.getGstin().isBlank() ? null : request.getGstin().toUpperCase());
        tenant.setAddressLine1(request.getAddressLine1());
        tenant.setAddressLine2(request.getAddressLine2());
        tenant.setCity(request.getCity());
        tenant.setState(request.getState());
        tenant.setPincode(request.getPincode());
        tenant.setFinancialYearStartMonth(request.getFinancialYearStartMonth() == 0 ? 4 : request.getFinancialYearStartMonth());
        tenant.setBusinessType(request.getBusinessType());
        return mapToResponse(tenantRepository.save(tenant));
    }

    private Tenant currentTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    }

    private CompanyDetailsResponse mapToResponse(Tenant tenant) {
        return CompanyDetailsResponse.builder()
                .portalId(tenant.getPortalId())
                .name(tenant.getName())
                .legalName(tenant.getLegalName())
                .gstin(tenant.getGstin())
                .addressLine1(tenant.getAddressLine1())
                .addressLine2(tenant.getAddressLine2())
                .city(tenant.getCity())
                .state(tenant.getState())
                .pincode(tenant.getPincode())
                .financialYearStartMonth(tenant.getFinancialYearStartMonth())
                .businessType(tenant.getBusinessType())
                .build();
    }
}
