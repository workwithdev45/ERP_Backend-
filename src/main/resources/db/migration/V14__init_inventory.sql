-- Matches the (previously uncommitted, unmigrated) inventory entities exactly — their tables
-- had only ever been created locally via Hibernate's ddl-auto, never through Flyway, which is
-- exactly the schema-drift risk the dev profile fix (G22) exists to catch.
CREATE TABLE products (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(50) NOT NULL,
    sku           VARCHAR(100) NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    description   VARCHAR(500),
    category      VARCHAR(100),
    uom           VARCHAR(50),
    reorder_level INTEGER NOT NULL DEFAULT 0,
    active        BOOLEAN DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    version       BIGINT
);

CREATE TABLE warehouses (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    code              VARCHAR(50) NOT NULL UNIQUE,
    location          VARCHAR(255),
    default_warehouse BOOLEAN DEFAULT false,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ,
    version           BIGINT
);

CREATE TABLE inventory_items (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(50) NOT NULL,
    product_id          BIGINT NOT NULL REFERENCES products(id),
    warehouse_id        BIGINT NOT NULL REFERENCES warehouses(id),
    available_quantity  INTEGER NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT,
    CONSTRAINT uq_inventory_items_product_warehouse UNIQUE (product_id, warehouse_id)
);

CREATE TABLE stock_movements (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id      VARCHAR(50) NOT NULL,
    product_id     BIGINT NOT NULL REFERENCES products(id),
    warehouse_id   BIGINT NOT NULL REFERENCES warehouses(id),
    movement_type  VARCHAR(30) NOT NULL,
    quantity       INTEGER NOT NULL,
    reference_type VARCHAR(50),
    reference_id   VARCHAR(100),
    reason         VARCHAR(255),
    performed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ,
    version        BIGINT
);

CREATE INDEX idx_products_tenant_id ON products(tenant_id);
CREATE INDEX idx_warehouses_tenant_id ON warehouses(tenant_id);
CREATE INDEX idx_inventory_items_tenant_id ON inventory_items(tenant_id);
CREATE INDEX idx_stock_movements_tenant_id ON stock_movements(tenant_id);
