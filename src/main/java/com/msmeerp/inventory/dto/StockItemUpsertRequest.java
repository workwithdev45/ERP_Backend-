package com.msmeerp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
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
public class StockItemUpsertRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Item name is required")
    private String name;

    private String category;

    @NotBlank(message = "Unit of measure is required")
    private String uom;

    @NotNull(message = "Reorder threshold is required")
    private BigDecimal reorderThreshold;

    private Boolean active;
}
