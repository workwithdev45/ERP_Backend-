package com.msmeerp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemDto {
    private Long id;
    private String sku;
    private String name;
    private String category;
    private String uom;
    private BigDecimal reorderThreshold;
    private boolean active;
    private BigDecimal currentStock;
    private boolean lowStock;
}
