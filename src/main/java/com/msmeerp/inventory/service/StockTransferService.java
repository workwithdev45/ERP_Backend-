package com.msmeerp.inventory.service;

import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.dto.StockTransferDto;
import com.msmeerp.inventory.dto.StockTransferRequest;
import org.springframework.data.domain.Pageable;

public interface StockTransferService {
    StockTransferDto createTransfer(StockTransferRequest request);
    StockTransferDto completeTransfer(Long id);
    StockTransferDto cancelTransfer(Long id);
    PagedResponse<StockTransferDto> getAllTransfers(Pageable pageable);
}
