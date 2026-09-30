package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** W10: an item at or below its reorder level once open purchase orders are counted. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSuggestionDto {
    private Long productId;
    private String productName;
    private String sku;
    private String uom;
    private Integer reorderLevel;
    private Integer available;
    private Integer onOrder;
    private Integer suggestedQuantity;
    private BigDecimal lastRate;
    private BigDecimal gstRate;
    private Long lastVendorId;
    private String lastVendorName;
}
