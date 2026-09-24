package com.msmeerp.inventory.service;

import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.dto.RecordMovementRequest;
import com.msmeerp.inventory.dto.StockMovementDto;
import org.springframework.data.domain.Pageable;

public interface StockMovementService {
    StockMovementDto recordMovement(RecordMovementRequest request);
    PagedResponse<StockMovementDto> getMovements(Long stockItemId, Long warehouseId, Pageable pageable);
}
