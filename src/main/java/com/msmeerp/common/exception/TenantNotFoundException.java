package com.msmeerp.common.exception;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String tenantId) {
        super(String.format("Tenant '%s' is invalid or not registered in the system", tenantId));
    }
}
