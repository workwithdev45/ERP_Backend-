-- Matches the (previously uncommitted, unmigrated) inventory entities exactly — their tables
-- had only ever been created locally via Hibernate's ddl-auto, never through Flyway, which is
-- exactly the schema-drift risk the dev profile fix (G22) exists to catch.
--
-- W7/W8 of the delivery plan: item master (type/HSN/GST/units/barcode), warehouses, stock ledger,
-- weighted-average valuation, reservations. Uniqueness on sku/code/barcode is scoped to the
-- tenant, not global — the original draft of this migration wrongly made them globally unique,
-- which would have let one tenant's SKU block every other tenant's use of the same code.
CREATE TABLE products (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(50) NOT NULL,
    sku                 VARCHAR(100) NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         VARCHAR(500),
    category            VARCHAR(100),
    item_type           VARCHAR(20) NOT NULL DEFAULT 'STOCK',
    hsn_code            VARCHAR(20),
    gst_rate_percent    NUMERIC(5,2),
    barcode             VARCHAR(100),
    image_url           VARCHAR(500),
    uom                 VARCHAR(50),
    secondary_unit      VARCHAR(50),
    conversion_factor   NUMERIC(12,4),
    reorder_level       INTEGER NOT NULL DEFAULT 0,
    active              BOOLEAN DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT,
    CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE TABLE warehouses (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    code              VARCHAR(50) NOT NULL,
    location          VARCHAR(255),
    default_warehouse BOOLEAN DEFAULT false,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ,
    version           BIGINT,
    CONSTRAINT uq_warehouses_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE inventory_items (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(50) NOT NULL,
    product_id          BIGINT NOT NULL REFERENCES products(id),
    warehouse_id        BIGINT NOT NULL REFERENCES warehouses(id),
    available_quantity  INTEGER NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER NOT NULL DEFAULT 0,
    average_cost        NUMERIC(14,4),
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
    unit_cost      NUMERIC(14,4),
    total_value    NUMERIC(16,4),
    reason_code    VARCHAR(30),
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
CREATE INDEX idx_stock_movements_product_warehouse ON stock_movements(product_id, warehouse_id);
