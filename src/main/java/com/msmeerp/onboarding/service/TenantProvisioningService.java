package com.msmeerp.onboarding.service;

import com.msmeerp.tenant.entity.Tenant;

public interface TenantProvisioningService {

    Tenant provisionTenant(String adminEmail, String portalId, boolean startBlank);
}
