package com.msmeerp.inventory.service.impl;

import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.inventory.dto.StockBatchDto;
import com.msmeerp.inventory.dto.StockBatchUpsertRequest;
import com.msmeerp.inventory.entity.StockBatch;
import com.msmeerp.inventory.entity.StockItem;
import com.msmeerp.inventory.repository.StockBatchRepository;
import com.msmeerp.inventory.repository.StockItemRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.service.StockBatchService;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockBatchServiceImpl implements StockBatchService {

    private final StockBatchRepository stockBatchRepository;
    private final StockItemRepository stockItemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public StockBatchDto createBatch(StockBatchUpsertRequest request) {
        String tenantId = TenantContext.getTenantId();
        StockItem stockItem = findItemById(tenantId, request.getStockItemId());

        StockBatch batch = StockBatch.builder()
                .stockItem(stockItem)
                .batchNumber(request.getBatchNumber())
                .expiryDate(request.getExpiryDate())
                .manufacturedDate(request.getManufacturedDate())
                .build();
        batch.setTenantId(tenantId);

        StockBatch saved = stockBatchRepository.save(batch);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBatchDto getBatchById(Long id) {
        return mapToDto(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchDto> getAllBatches() {
        String tenantId = TenantContext.getTenantId();
        return stockBatchRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchDto> getBatchesForItem(Long stockItemId) {
        String tenantId = TenantContext.getTenantId();
        return stockBatchRepository.findByTenantIdAndStockItemId(tenantId, stockItemId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchDto> getExpiringSoon(int days) {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(days);

        return stockBatchRepository.findByTenantIdAndExpiryDateIsNotNullAndExpiryDateBetween(tenantId, today, horizon)
                .stream()
                .filter(batch -> stockMovementRepository.sumQuantityByStockBatch(tenantId, batch.getId())
                        .compareTo(BigDecimal.ZERO) > 0)
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public StockBatchDto updateBatch(Long id, StockBatchUpsertRequest request) {
        StockBatch batch = findById(id);
        String tenantId = TenantContext.getTenantId();

        if (!batch.getStockItem().getId().equals(request.getStockItemId())) {
            batch.setStockItem(findItemById(tenantId, request.getStockItemId()));
        }
        batch.setBatchNumber(request.getBatchNumber());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setManufacturedDate(request.getManufacturedDate());

        return mapToDto(stockBatchRepository.save(batch));
    }

    @Override
    @Transactional
    public void deleteBatch(Long id) {
        StockBatch batch = findById(id);
        stockBatchRepository.delete(batch);
    }

    private StockItem findItemById(String tenantId, Long id) {
        StockItem item = stockItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockItem", "id", id));
        if (item.getTenantId() != null && !item.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("StockItem", "id", id);
        }
        return item;
    }

    private StockBatch findById(Long id) {
        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockBatch", "id", id));

        String tenantId = TenantContext.getTenantId();
        if (batch.getTenantId() != null && !batch.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("StockBatch", "id", id);
        }
        return batch;
    }

    private StockBatchDto mapToDto(StockBatch batch) {
        return StockBatchDto.builder()
                .id(batch.getId())
                .stockItemId(batch.getStockItem().getId())
                .stockItemName(batch.getStockItem().getName())
                .batchNumber(batch.getBatchNumber())
                .expiryDate(batch.getExpiryDate())
                .manufacturedDate(batch.getManufacturedDate())
                .build();
    }
}
