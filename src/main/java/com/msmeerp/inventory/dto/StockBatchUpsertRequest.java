package com.msmeerp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchUpsertRequest {

    @NotNull(message = "Stock item ID is required")
    private Long stockItemId;

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    private LocalDate expiryDate;
    private LocalDate manufacturedDate;
}
