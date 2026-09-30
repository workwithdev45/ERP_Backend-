package com.msmeerp.trade.entity;

import com.msmeerp.inventory.entity.ItemType;
import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "trade_document_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDocumentLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private TradeDocument document;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "uom", length = 50)
    private String uom;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** How much a follow-on document has consumed: received (PO), billed (GRN), delivered (SO), invoiced (challan), returned (bill/invoice). */
    @Column(name = "fulfilled_quantity", nullable = false)
    @Builder.Default
    private Integer fulfilledQuantity = 0;

    /** Sales orders only: units currently reserved in the order's warehouse. */
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal rate;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = BigDecimal.ZERO;

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

    @Column(name = "line_total", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "source_line_id")
    private Long sourceLineId;

    public int getPendingQuantity() {
        return quantity - fulfilledQuantity;
    }

    public boolean isStockItem() {
        return itemType == ItemType.STOCK;
    }
}
