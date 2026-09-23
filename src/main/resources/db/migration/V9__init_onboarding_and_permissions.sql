CREATE TABLE company_onboarding (
    onboarding_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_email        VARCHAR(256) NOT NULL UNIQUE,
    admin_phone        VARCHAR(20),
    otp                VARCHAR(6),
    otp_valid_until    BIGINT,
    otp_attempts       INTEGER DEFAULT 0,
    last_otp_sent_at   TIMESTAMPTZ,
    registration_token VARCHAR(2048),
    portal_id          VARCHAR(128),
    status             VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ
);

CREATE TABLE user_tenant_map (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      VARCHAR(150) NOT NULL UNIQUE,
    tenant_ids TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    version    BIGINT
);

CREATE TABLE user_module_permissions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(50) NOT NULL,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_code VARCHAR(30) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    version     BIGINT,
    CONSTRAINT uq_user_module_permissions UNIQUE (user_id, module_code)
);

CREATE TABLE user_module_permission_actions (
    user_module_permission_id BIGINT NOT NULL REFERENCES user_module_permissions(id) ON DELETE CASCADE,
    action                    VARCHAR(20) NOT NULL
);

CREATE INDEX idx_user_module_permissions_tenant_id ON user_module_permissions(tenant_id);
CREATE INDEX idx_user_module_permissions_user_id ON user_module_permissions(user_id);
