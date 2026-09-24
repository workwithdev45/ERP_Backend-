package com.msmeerp.inventory.service.impl;

import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.dto.RecordMovementRequest;
import com.msmeerp.inventory.dto.StockMovementDto;
import com.msmeerp.inventory.entity.StockBatch;
import com.msmeerp.inventory.entity.StockItem;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.StockBatchRepository;
import com.msmeerp.inventory.repository.StockItemRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.repository.WarehouseRepository;
import com.msmeerp.inventory.service.StockMovementService;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final StockItemRepository stockItemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public StockMovementDto recordMovement(RecordMovementRequest request) {
        String tenantId = TenantContext.getTenantId();

        StockItem stockItem = findItemById(tenantId, request.getStockItemId());
        Warehouse warehouse = findWarehouseById(tenantId, request.getWarehouseId());
        StockBatch stockBatch = request.getStockBatchId() != null
                ? findBatchById(tenantId, request.getStockBatchId())
                : null;

        StockMovement movement = StockMovement.builder()
                .stockItem(stockItem)
                .stockBatch(stockBatch)
                .warehouse(warehouse)
                .quantity(request.getQuantity())
                .movementType(request.getMovementType())
                .referenceNote(request.getReferenceNote())
                .build();
        movement.setTenantId(tenantId);

        StockMovement saved = stockMovementRepository.save(movement);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StockMovementDto> getMovements(Long stockItemId, Long warehouseId, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<StockMovement> page;

        if (stockItemId != null && warehouseId != null) {
            page = stockMovementRepository.findByTenantIdAndStockItemIdAndWarehouseId(tenantId, stockItemId, warehouseId, pageable);
        } else if (stockItemId != null) {
            page = stockMovementRepository.findByTenantIdAndStockItemId(tenantId, stockItemId, pageable);
        } else if (warehouseId != null) {
            page = stockMovementRepository.findByTenantIdAndWarehouseId(tenantId, warehouseId, pageable);
        } else {
            page = stockMovementRepository.findByTenantId(tenantId, pageable);
        }

        return PagedResponse.from(page.map(this::mapToDto));
    }

    private StockItem findItemById(String tenantId, Long id) {
        StockItem item = stockItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockItem", "id", id));
        if (item.getTenantId() != null && !item.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("StockItem", "id", id);
        }
        return item;
    }

    private StockBatch findBatchById(String tenantId, Long id) {
        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockBatch", "id", id));
        if (batch.getTenantId() != null && !batch.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("StockBatch", "id", id);
        }
        return batch;
    }

    private Warehouse findWarehouseById(String tenantId, Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
        if (warehouse.getTenantId() != null && !warehouse.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Warehouse", "id", id);
        }
        return warehouse;
    }

    private StockMovementDto mapToDto(StockMovement movement) {
        return StockMovementDto.builder()
                .id(movement.getId())
                .stockItemId(movement.getStockItem().getId())
                .stockItemName(movement.getStockItem().getName())
                .stockBatchId(movement.getStockBatch() != null ? movement.getStockBatch().getId() : null)
                .batchNumber(movement.getStockBatch() != null ? movement.getStockBatch().getBatchNumber() : null)
                .warehouseId(movement.getWarehouse().getId())
                .warehouseName(movement.getWarehouse().getName())
                .quantity(movement.getQuantity())
                .movementType(movement.getMovementType())
                .referenceNote(movement.getReferenceNote())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
