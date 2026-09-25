package com.msmeerp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustmentRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Warehouse id is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String reason;
}
