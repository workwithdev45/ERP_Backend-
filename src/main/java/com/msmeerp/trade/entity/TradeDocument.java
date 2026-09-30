package com.msmeerp.trade.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * W6 document engine: the shared header for every purchase and sales document. Party details are
 * snapshotted so a posted document never changes when the party master is edited.
 */
@Entity
@Table(name = "trade_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDocument extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    private DocumentType docType;

    @Column(name = "doc_number", nullable = false, length = 30)
    private String docNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DocumentStatus status;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "party_name", nullable = false, length = 150)
    private String partyName;

    @Column(name = "party_gstin", length = 15)
    private String partyGstin;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    /** Expected delivery (PO/SO), valid-until (quotation) or payment due date (bill/invoice). */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    /** The document this one was created from, e.g. the PO behind a GRN. */
    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    /** The party's own number: vendor invoice/challan no., customer PO no. */
    @Column(name = "party_reference", length = 100)
    private String partyReference;

    @Column(name = "place_of_supply", length = 100)
    private String placeOfSupply;

    @Column(name = "inter_state", nullable = false)
    @Builder.Default
    private Boolean interState = false;

    /** Purchase bills only: GST is paid by us to the government, not to the vendor. */
    @Column(name = "reverse_charge", nullable = false)
    @Builder.Default
    private Boolean reverseCharge = false;

    @Column(name = "taxable_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "cgst_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "round_off", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Bills/invoices: paid/received so far plus debit/credit notes applied. */
    @Column(name = "settled_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    @Builder.Default
    private List<TradeDocumentLine> lines = new ArrayList<>();

    public BigDecimal getBalance() {
        return totalAmount.subtract(settledAmount);
    }

    public void addLine(TradeDocumentLine line) {
        line.setDocument(this);
        line.setLineNo(lines.size() + 1);
        lines.add(line);
    }
}
