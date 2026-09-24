package com.msmeerp.inventory.service;

import com.msmeerp.inventory.dto.StockBatchDto;
import com.msmeerp.inventory.dto.StockBatchUpsertRequest;

import java.util.List;

public interface StockBatchService {
    StockBatchDto createBatch(StockBatchUpsertRequest request);
    StockBatchDto getBatchById(Long id);
    List<StockBatchDto> getAllBatches();
    List<StockBatchDto> getBatchesForItem(Long stockItemId);
    List<StockBatchDto> getExpiringSoon(int days);
    StockBatchDto updateBatch(Long id, StockBatchUpsertRequest request);
    void deleteBatch(Long id);
}
