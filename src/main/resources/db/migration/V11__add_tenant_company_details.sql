-- G3: onboarding never captured the legal/GST details needed to produce a correct GST invoice.
ALTER TABLE tenants
    ADD COLUMN legal_name          VARCHAR(200),
    ADD COLUMN gstin               VARCHAR(15),
    ADD COLUMN address_line1       VARCHAR(200),
    ADD COLUMN address_line2       VARCHAR(200),
    ADD COLUMN city                VARCHAR(100),
    ADD COLUMN state                VARCHAR(100),
    ADD COLUMN pincode             VARCHAR(10),
    ADD COLUMN financial_year_start_month INTEGER NOT NULL DEFAULT 4,
    ADD COLUMN business_type       VARCHAR(30),
    ADD COLUMN terms_accepted_at   TIMESTAMPTZ,
    ADD COLUMN terms_version       VARCHAR(20);
