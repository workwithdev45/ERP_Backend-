CREATE TABLE tenants (
    id                    VARCHAR(50) PRIMARY KEY,
    name                  VARCHAR(150) NOT NULL,
    display_name          VARCHAR(150),
    subdomain             VARCHAR(50) UNIQUE,
    portal_id             VARCHAR(50) UNIQUE,
    database_name         VARCHAR(100),
    admin_email           VARCHAR(150),
    subscription_tier     VARCHAR(50) DEFAULT 'Standard',
    timezone              VARCHAR(50) DEFAULT 'Asia/Kolkata',
    locale                VARCHAR(20) DEFAULT 'en_IN',
    currency              VARCHAR(10) DEFAULT 'INR',
    first_login_pending   BOOLEAN NOT NULL DEFAULT true,
    active                BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ
);

CREATE TABLE tenant_settings (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(50) NOT NULL REFERENCES tenants(id),
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    version       BIGINT
);

CREATE INDEX idx_tenant_settings_tenant_id ON tenant_settings(tenant_id);
