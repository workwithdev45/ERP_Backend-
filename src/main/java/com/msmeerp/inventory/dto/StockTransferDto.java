package com.msmeerp.inventory.dto;

import com.msmeerp.inventory.entity.StockTransfer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferDto {
    private Long id;
    private Long sourceWarehouseId;
    private String sourceWarehouseName;
    private Long destinationWarehouseId;
    private String destinationWarehouseName;
    private Long stockItemId;
    private String stockItemName;
    private Long stockBatchId;
    private String batchNumber;
    private BigDecimal quantity;
    private StockTransfer.TransferStatus status;
    private Instant createdAt;
}
