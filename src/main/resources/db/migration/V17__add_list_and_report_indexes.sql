-- W15 performance: indexes behind the paged document/payment lists, reports and the stock ledger.
CREATE INDEX idx_trade_documents_list ON trade_documents(tenant_id, doc_type, id DESC);
CREATE INDEX idx_trade_documents_status ON trade_documents(tenant_id, doc_type, status);
CREATE INDEX idx_trade_documents_date ON trade_documents(tenant_id, doc_type, doc_date);
CREATE INDEX idx_payments_list ON payments(tenant_id, direction, id DESC);
CREATE INDEX idx_payments_date ON payments(tenant_id, payment_date);
CREATE INDEX idx_stock_movements_recent ON stock_movements(tenant_id, performed_at DESC);
CREATE INDEX idx_parties_tenant_name ON parties(tenant_id, name);
