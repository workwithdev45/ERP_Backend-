package com.msmeerp.accesscontrol.service;

import com.msmeerp.accesscontrol.dto.TenantModuleDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;

import java.util.List;

/** G14: per-tenant module on/off switches, backing Settings -> Modules. */
public interface TenantModuleService {
    List<TenantModuleDto> getModulesForCurrentTenant();
    TenantModuleDto setModuleEnabled(ModuleCode moduleCode, boolean enabled);
    boolean isModuleEnabled(String tenantId, ModuleCode moduleCode);

    /** Seeds every module as enabled for a newly provisioned tenant. */
    void initializeDefaultModules(String tenantId);

    /** Used during provisioning, before the new tenant is the request's active tenant context. */
    void setModuleEnabledForTenant(String tenantId, ModuleCode moduleCode, boolean enabled);
}
