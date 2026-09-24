package com.msmeerp.inventory.service;

import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.dto.StockItemDto;
import com.msmeerp.inventory.dto.StockItemUpsertRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface StockItemService {
    StockItemDto createItem(StockItemUpsertRequest request);
    StockItemDto getItemById(Long id);
    PagedResponse<StockItemDto> getAllItems(Pageable pageable);
    List<StockItemDto> getLowStockItems();
    StockItemDto updateItem(Long id, StockItemUpsertRequest request);
    void deleteItem(Long id);
    BigDecimal computeCurrentStock(Long itemId);
    BigDecimal computeCurrentStock(Long itemId, Long warehouseId);
}
