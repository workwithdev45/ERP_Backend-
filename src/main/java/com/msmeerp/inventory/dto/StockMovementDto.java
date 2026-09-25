package com.msmeerp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementDto {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private Long warehouseId;
    private String warehouseName;
    private String movementType;
    private Integer quantity;
    private String referenceType;
    private String referenceId;
    private String reason;
    private Instant performedAt;
}
