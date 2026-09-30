package com.msmeerp.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Weighted-average stock valuation per item and warehouse. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValuationDto {
    private List<StockValuationRowDto> rows;
    private BigDecimal totalValue;
    /** Rows with stock but no cost recorded — valued at zero. */
    private int rowsWithoutCost;
}
