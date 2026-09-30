package com.msmeerp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private Long warehouseId;
    private String warehouseName;

    /** Total physical quantity on hand in this warehouse. */
    private Integer availableQuantity;

    /** Committed to open sales orders etc. — not yet shipped. */
    private Integer reservedQuantity;

    /** availableQuantity - reservedQuantity: what can still be newly committed. */
    private Integer availableToPromise;

    private BigDecimal averageCost;
    private Integer reorderLevel;
    private Boolean lowStock;
}
