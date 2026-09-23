package com.msmeerp.tenant.service;

import com.msmeerp.tenant.dto.TenantRequest;
import com.msmeerp.tenant.dto.TenantResponse;

import java.util.List;

public interface TenantResolverService {
    boolean isValidTenant(String tenantId);
    String getTenantName(String tenantId);
    TenantResponse createTenant(TenantRequest request);
    TenantResponse getTenantById(String tenantId);
    List<TenantResponse> getAllTenants();
}
