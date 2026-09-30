package com.msmeerp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** W8: move stock between two warehouses as one atomic OUT + IN, recorded as a single TRANSFER pair. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Source warehouse id is required")
    private Long fromWarehouseId;

    @NotNull(message = "Destination warehouse id is required")
    private Long toWarehouseId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private String reason;
}
