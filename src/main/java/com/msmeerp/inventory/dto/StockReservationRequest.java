package com.msmeerp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** W8: reserve/release stock against a pending commitment (e.g. an open sales order) without moving it yet. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Warehouse id is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}
