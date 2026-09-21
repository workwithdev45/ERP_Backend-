package com.hms.tenant.service;

import com.hms.tenant.dto.TenantRequest;
import com.hms.tenant.dto.TenantResponse;

import java.util.List;

public interface TenantResolverService {
    boolean isValidTenant(String tenantId);
    String getTenantName(String tenantId);
    TenantResponse createTenant(TenantRequest request);
    TenantResponse getTenantById(String tenantId);
    List<TenantResponse> getAllTenants();
}
