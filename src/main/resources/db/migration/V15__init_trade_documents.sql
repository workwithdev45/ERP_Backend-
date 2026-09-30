-- W5 masters (parties) + W6 document engine + W9–W12 purchase & sales.
--
-- Every purchase and sales document (PO, GRN, bill, debit note, quotation, sales order, delivery
-- challan, invoice, credit note) shares one header table and one line table, distinguished by
-- doc_type — the "document header/lines base" of the delivery plan. Party and product details are
-- snapshotted onto the document so a posted document never changes when the master is edited.

CREATE TABLE parties (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(50)  NOT NULL,
    party_type          VARCHAR(20)  NOT NULL,
    name                VARCHAR(150) NOT NULL,
    gstin               VARCHAR(15),
    phone               VARCHAR(20),
    email               VARCHAR(150),
    address_line1       VARCHAR(200),
    city                VARCHAR(100),
    state               VARCHAR(100),
    pincode             VARCHAR(10),
    payment_terms_days  INTEGER      NOT NULL DEFAULT 30,
    credit_limit        NUMERIC(16,2),
    active              BOOLEAN      NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT
);

-- Per-tenant running number for each document type (PO-0001, INV-0001, ...).
CREATE TABLE document_sequences (
    tenant_id    VARCHAR(50) NOT NULL,
    doc_type     VARCHAR(30) NOT NULL,
    next_number  BIGINT      NOT NULL,
    PRIMARY KEY (tenant_id, doc_type)
);

CREATE TABLE trade_documents (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(50)  NOT NULL,
    doc_type            VARCHAR(30)  NOT NULL,
    doc_number          VARCHAR(30)  NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    party_id            BIGINT       NOT NULL REFERENCES parties(id),
    party_name          VARCHAR(150) NOT NULL,
    party_gstin         VARCHAR(15),
    doc_date            DATE         NOT NULL,
    due_date            DATE,
    warehouse_id        BIGINT       REFERENCES warehouses(id),
    source_document_id  BIGINT       REFERENCES trade_documents(id),
    party_reference     VARCHAR(100),
    place_of_supply     VARCHAR(100),
    inter_state         BOOLEAN      NOT NULL DEFAULT false,
    reverse_charge      BOOLEAN      NOT NULL DEFAULT false,
    taxable_amount      NUMERIC(16,2) NOT NULL DEFAULT 0,
    cgst_amount         NUMERIC(16,2) NOT NULL DEFAULT 0,
    sgst_amount         NUMERIC(16,2) NOT NULL DEFAULT 0,
    igst_amount         NUMERIC(16,2) NOT NULL DEFAULT 0,
    round_off           NUMERIC(8,2)  NOT NULL DEFAULT 0,
    total_amount        NUMERIC(16,2) NOT NULL DEFAULT 0,
    settled_amount      NUMERIC(16,2) NOT NULL DEFAULT 0,
    notes               VARCHAR(500),
    approved_at         TIMESTAMPTZ,
    approved_by         VARCHAR(100),
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT,
    CONSTRAINT uq_trade_documents_number UNIQUE (tenant_id, doc_type, doc_number)
);

CREATE TABLE trade_document_lines (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(50)  NOT NULL,
    document_id         BIGINT       NOT NULL REFERENCES trade_documents(id) ON DELETE CASCADE,
    line_no             INTEGER      NOT NULL,
    product_id          BIGINT       NOT NULL REFERENCES products(id),
    product_name        VARCHAR(150) NOT NULL,
    sku                 VARCHAR(100),
    hsn_code            VARCHAR(20),
    uom                 VARCHAR(50),
    item_type           VARCHAR(20)  NOT NULL,
    quantity            INTEGER      NOT NULL,
    -- How much of this line a follow-on document has consumed (received, delivered, billed, invoiced).
    fulfilled_quantity  INTEGER      NOT NULL DEFAULT 0,
    -- Sales orders only: stock currently held for this line.
    reserved_quantity   INTEGER      NOT NULL DEFAULT 0,
    rate                NUMERIC(14,2) NOT NULL,
    discount_percent    NUMERIC(5,2)  NOT NULL DEFAULT 0,
    gst_rate            NUMERIC(5,2)  NOT NULL DEFAULT 0,
    taxable_amount      NUMERIC(16,2) NOT NULL DEFAULT 0,
    cgst_amount         NUMERIC(16,2) NOT NULL DEFAULT 0,
    sgst_amount         NUMERIC(16,2) NOT NULL DEFAULT 0,
    igst_amount         NUMERIC(16,2) NOT NULL DEFAULT 0,
    line_total          NUMERIC(16,2) NOT NULL DEFAULT 0,
    source_line_id      BIGINT       REFERENCES trade_document_lines(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT
);

CREATE TABLE payments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id       VARCHAR(50)   NOT NULL,
    payment_number  VARCHAR(30)   NOT NULL,
    direction       VARCHAR(20)   NOT NULL,
    party_id        BIGINT        NOT NULL REFERENCES parties(id),
    party_name      VARCHAR(150)  NOT NULL,
    payment_date    DATE          NOT NULL,
    amount          NUMERIC(16,2) NOT NULL,
    mode            VARCHAR(20)   NOT NULL,
    reference       VARCHAR(100),
    notes           VARCHAR(500),
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,
    version         BIGINT,
    CONSTRAINT uq_payments_number UNIQUE (tenant_id, direction, payment_number)
);

CREATE TABLE payment_allocations (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id    VARCHAR(50)   NOT NULL,
    payment_id   BIGINT        NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    document_id  BIGINT        NOT NULL REFERENCES trade_documents(id),
    amount       NUMERIC(16,2) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ,
    version      BIGINT
);

CREATE INDEX idx_parties_tenant_type ON parties(tenant_id, party_type);
CREATE INDEX idx_trade_documents_tenant_type ON trade_documents(tenant_id, doc_type);
CREATE INDEX idx_trade_documents_party ON trade_documents(party_id);
CREATE INDEX idx_trade_documents_source ON trade_documents(source_document_id);
CREATE INDEX idx_trade_document_lines_document ON trade_document_lines(document_id);
CREATE INDEX idx_trade_document_lines_product ON trade_document_lines(product_id);
CREATE INDEX idx_payments_tenant_direction ON payments(tenant_id, direction);
CREATE INDEX idx_payment_allocations_payment ON payment_allocations(payment_id);
CREATE INDEX idx_payment_allocations_document ON payment_allocations(document_id);
