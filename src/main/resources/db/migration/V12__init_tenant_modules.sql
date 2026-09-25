-- G14: modules were always visible with no per-company on/off switch. Every existing tenant is
-- seeded with all current modules enabled so nothing changes for them until an Admin flips one off.
CREATE TABLE tenant_modules (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(50) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    module_code VARCHAR(30) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    version     BIGINT,
    CONSTRAINT uq_tenant_modules UNIQUE (tenant_id, module_code)
);

CREATE INDEX idx_tenant_modules_tenant_id ON tenant_modules(tenant_id);

INSERT INTO tenant_modules (tenant_id, module_code, enabled)
SELECT t.id, m.module_code, true
FROM tenants t
CROSS JOIN (VALUES
    ('SALES'), ('PURCHASE'), ('INVENTORY'), ('PRODUCTION'),
    ('ACCOUNTS'), ('CRM'), ('HR'), ('REPORTS'), ('SETTINGS')
) AS m(module_code)
ON CONFLICT (tenant_id, module_code) DO NOTHING;
