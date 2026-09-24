package com.msmeerp.inventory.dto;

import com.msmeerp.inventory.entity.StockMovement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDto {
    private Long id;
    private Long stockItemId;
    private String stockItemName;
    private Long stockBatchId;
    private String batchNumber;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal quantity;
    private StockMovement.MovementType movementType;
    private String referenceNote;
    private Instant createdAt;
}
