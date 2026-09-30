-- W13 compliance & sharing: e-invoice (IRN/QR) and e-way bill per sales invoice, obtained through a
-- GSP (a sandbox provider until a real GSP is configured), plus a log of payment reminders sent.

CREATE TABLE einvoices (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    -- One IRN per invoice, ever: the IRP never re-issues an IRN for a document number, even after cancellation.
    document_id     BIGINT       NOT NULL UNIQUE REFERENCES trade_documents(id),
    provider        VARCHAR(30)  NOT NULL,
    irn             VARCHAR(64)  NOT NULL,
    ack_no          VARCHAR(20)  NOT NULL,
    ack_date        TIMESTAMPTZ  NOT NULL,
    signed_qr       TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    cancel_reason   VARCHAR(150),
    cancelled_at    TIMESTAMPTZ,
    request_json    TEXT         NOT NULL,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,
    version         BIGINT
);

CREATE TABLE eway_bills (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id         VARCHAR(50)  NOT NULL,
    document_id       BIGINT       NOT NULL REFERENCES trade_documents(id),
    provider          VARCHAR(30)  NOT NULL,
    ewb_no            VARCHAR(20)  NOT NULL,
    ewb_date          TIMESTAMPTZ  NOT NULL,
    valid_until       TIMESTAMPTZ  NOT NULL,
    transport_mode    VARCHAR(10)  NOT NULL,
    distance_km       INTEGER      NOT NULL,
    vehicle_no        VARCHAR(20),
    transporter_id    VARCHAR(15),
    transporter_name  VARCHAR(100),
    transport_doc_no  VARCHAR(30),
    status            VARCHAR(20)  NOT NULL,
    cancel_reason     VARCHAR(150),
    cancelled_at      TIMESTAMPTZ,
    created_by        VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ,
    version           BIGINT
);

CREATE TABLE payment_reminders (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(50)  NOT NULL,
    document_id   BIGINT       NOT NULL REFERENCES trade_documents(id),
    party_id      BIGINT       NOT NULL REFERENCES parties(id),
    channel       VARCHAR(20)  NOT NULL,
    recipient     VARCHAR(30),
    trigger_type  VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    message       TEXT         NOT NULL,
    error         VARCHAR(255),
    sent_at       TIMESTAMPTZ  NOT NULL,
    created_by    VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    version       BIGINT
);

CREATE INDEX idx_einvoices_tenant ON einvoices(tenant_id);
CREATE INDEX idx_eway_bills_document ON eway_bills(document_id);
CREATE INDEX idx_payment_reminders_document ON payment_reminders(document_id);
CREATE INDEX idx_payment_reminders_tenant_sent ON payment_reminders(tenant_id, sent_at);
