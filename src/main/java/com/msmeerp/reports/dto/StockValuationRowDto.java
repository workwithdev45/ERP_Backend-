package com.msmeerp.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValuationRowDto {
    private Long productId;
    private String sku;
    private String productName;
    private String category;
    private String warehouseName;
    private String uom;
    private Integer quantity;
    private Integer reserved;
    private BigDecimal averageCost;
    private BigDecimal value;
}
