package com.msmeerp.accesscontrol.entity;

/**
 * The fixed set of ERP modules a tenant Admin can grant per-user access to.
 * Kept as an enum for the MVP rather than a database-backed catalog table
 * (see architecture plan's "modules" table) — promote to a table once
 * tenants need to enable/disable modules per subscription plan.
 */
public enum ModuleCode {
    SALES,
    PURCHASE,
    INVENTORY,
    PRODUCTION,
    ACCOUNTS,
    CRM,
    HR,
    REPORTS,
    SETTINGS
}
