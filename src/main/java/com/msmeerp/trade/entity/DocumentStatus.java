package com.msmeerp.trade.entity;

/**
 * Status lifecycle across document types. Each type uses a subset:
 * PO: DRAFT → APPROVED → PARTIALLY_RECEIVED → RECEIVED (or CANCELLED);
 * GRN: POSTED → BILLED; bill/invoice: UNPAID → PARTIALLY_PAID → PAID;
 * quotation: OPEN → CONVERTED (or CANCELLED);
 * sales order: CONFIRMED → PARTIALLY_DELIVERED → DELIVERED (or CANCELLED);
 * delivery challan: POSTED → INVOICED; debit/credit note: POSTED.
 */
public enum DocumentStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_RECEIVED,
    RECEIVED,
    OPEN,
    CONVERTED,
    CONFIRMED,
    PARTIALLY_DELIVERED,
    DELIVERED,
    POSTED,
    BILLED,
    INVOICED,
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}
