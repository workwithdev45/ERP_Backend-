package com.hms.tenant.context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    private TenantContext() {}

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setTenantId(String tenantId) {
        log.trace("Setting current tenant to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        log.trace("Clearing current tenant");
        CURRENT_TENANT.remove();
    }
}
