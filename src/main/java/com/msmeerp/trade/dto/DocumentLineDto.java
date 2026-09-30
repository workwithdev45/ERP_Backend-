package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentLineDto {
    private Long id;
    private Integer lineNo;
    private Long productId;
    private String productName;
    private String sku;
    private String hsnCode;
    private String uom;
    private String itemType;
    private Integer quantity;
    private Integer fulfilledQuantity;
    private Integer pendingQuantity;
    private Integer reservedQuantity;
    private BigDecimal rate;
    private BigDecimal discountPercent;
    private BigDecimal gstRate;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal lineTotal;
    private Long sourceLineId;
}
