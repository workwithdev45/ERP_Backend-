package com.msmeerp.inventory.dto;

import com.msmeerp.inventory.entity.StockMovement;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordMovementRequest {

    @NotNull(message = "Stock item ID is required")
    private Long stockItemId;

    private Long stockBatchId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    @NotNull(message = "Movement type is required")
    private StockMovement.MovementType movementType;

    private String referenceNote;
}
