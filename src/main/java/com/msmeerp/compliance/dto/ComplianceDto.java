package com.msmeerp.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** E-invoice and e-way bill state of one sales invoice, with why an action isn't available. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceDto {
    private Long documentId;
    private EInvoiceDto einvoice;
    private List<EwayBillDto> ewayBills;
    /** Null when an IRN can be generated; otherwise the reason it can't. */
    private String einvoiceBlockedReason;
    /** Null when an e-way bill can be generated; otherwise the reason it can't. */
    private String ewayBillBlockedReason;
    /** Value of goods (excl. services) moving on this invoice. */
    private BigDecimal goodsValue;
    /** An e-way bill is mandatory when goods worth more than ₹50,000 move. */
    private boolean ewayBillRequired;
}
