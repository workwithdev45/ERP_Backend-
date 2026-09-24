CREATE TABLE role_module_permissions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(50) NOT NULL,
    role_id     BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    module_code VARCHAR(30) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    version     BIGINT,
    CONSTRAINT uq_role_module_permissions UNIQUE (role_id, module_code)
);

CREATE TABLE role_module_permission_actions (
    role_module_permission_id BIGINT NOT NULL REFERENCES role_module_permissions(id) ON DELETE CASCADE,
    action                    VARCHAR(20) NOT NULL
);

CREATE INDEX idx_role_module_permissions_tenant_id ON role_module_permissions(tenant_id);
CREATE INDEX idx_role_module_permissions_role_id ON role_module_permissions(role_id);
