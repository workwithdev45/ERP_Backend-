package com.msmeerp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchDto {
    private Long id;
    private Long stockItemId;
    private String stockItemName;
    private String batchNumber;
    private LocalDate expiryDate;
    private LocalDate manufacturedDate;
}
