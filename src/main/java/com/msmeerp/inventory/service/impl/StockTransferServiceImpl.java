package com.msmeerp.inventory.service.impl;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.dto.StockTransferDto;
import com.msmeerp.inventory.dto.StockTransferRequest;
import com.msmeerp.inventory.entity.StockBatch;
import com.msmeerp.inventory.entity.StockItem;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.entity.StockTransfer;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.StockBatchRepository;
import com.msmeerp.inventory.repository.StockItemRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.repository.StockTransferRepository;
import com.msmeerp.inventory.repository.WarehouseRepository;
import com.msmeerp.inventory.service.StockTransferService;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockItemRepository stockItemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public StockTransferDto createTransfer(StockTransferRequest request) {
        String tenantId = TenantContext.getTenantId();

        if (request.getSourceWarehouseId().equals(request.getDestinationWarehouseId())) {
            throw new BadRequestException("Source and destination warehouse must be different");
        }

        Warehouse source = findWarehouseById(tenantId, request.getSourceWarehouseId());
        Warehouse destination = findWarehouseById(tenantId, request.getDestinationWarehouseId());
        StockItem stockItem = findItemById(tenantId, request.getStockItemId());
        StockBatch stockBatch = request.getStockBatchId() != null
                ? findBatchById(tenantId, request.getStockBatchId())
                : null;

        StockTransfer transfer = StockTransfer.builder()
                .sourceWarehouse(source)
                .destinationWarehouse(destination)
                .stockItem(stockItem)
                .stockBatch(stockBatch)
                .quantity(request.getQuantity())
                .status(StockTransfer.TransferStatus.PENDING)
                .build();
        transfer.setTenantId(tenantId);

        StockTransfer saved = stockTransferRepository.save(transfer);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public StockTransferDto completeTransfer(Long id) {
        StockTransfer transfer = findById(id);

        if (transfer.getStatus() != StockTransfer.TransferStatus.PENDING) {
            throw new BadRequestException("Only pending transfers can be completed");
        }

        String tenantId = TenantContext.getTenantId();

        StockMovement outMovement = StockMovement.builder()
                .stockItem(transfer.getStockItem())
                .stockBatch(transfer.getStockBatch())
                .warehouse(transfer.getSourceWarehouse())
                .quantity(transfer.getQuantity().negate())
                .movementType(StockMovement.MovementType.TRANSFER_OUT)
                .referenceNote("Stock transfer #" + transfer.getId())
                .build();
        outMovement.setTenantId(tenantId);
        stockMovementRepository.save(outMovement);

        StockMovement inMovement = StockMovement.builder()
                .stockItem(transfer.getStockItem())
                .stockBatch(transfer.getStockBatch())
                .warehouse(transfer.getDestinationWarehouse())
                .quantity(transfer.getQuantity())
                .movementType(StockMovement.MovementType.TRANSFER_IN)
                .referenceNote("Stock transfer #" + transfer.getId())
                .build();
        inMovement.setTenantId(tenantId);
        stockMovementRepository.save(inMovement);

        transfer.setStatus(StockTransfer.TransferStatus.COMPLETED);
        return mapToDto(stockTransferRepository.save(transfer));
    }

    @Override
    @Transactional
    public StockTransferDto cancelTransfer(Long id) {
        StockTransfer transfer = findById(id);

        if (transfer.getStatus() != StockTransfer.TransferStatus.PENDING) {
            throw new BadRequestException("Only pending transfers can be cancelled");
        }

        transfer.setStatus(StockTransfer.TransferStatus.CANCELLED);
        return mapToDto(stockTransferRepository.save(transfer));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StockTransferDto> getAllTransfers(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<StockTransfer> page = stockTransferRepository.findByTenantId(tenantId, pageable);
        return PagedResponse.from(page.map(this::mapToDto));
    }

    private StockTransfer findById(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", "id", id));

        String tenantId = TenantContext.getTenantId();
        if (transfer.getTenantId() != null && !transfer.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("StockTransfer", "id", id);
        }
        return transfer;
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

    private StockTransferDto mapToDto(StockTransfer transfer) {
        return StockTransferDto.builder()
                .id(transfer.getId())
                .sourceWarehouseId(transfer.getSourceWarehouse().getId())
                .sourceWarehouseName(transfer.getSourceWarehouse().getName())
                .destinationWarehouseId(transfer.getDestinationWarehouse().getId())
                .destinationWarehouseName(transfer.getDestinationWarehouse().getName())
                .stockItemId(transfer.getStockItem().getId())
                .stockItemName(transfer.getStockItem().getName())
                .stockBatchId(transfer.getStockBatch() != null ? transfer.getStockBatch().getId() : null)
                .batchNumber(transfer.getStockBatch() != null ? transfer.getStockBatch().getBatchNumber() : null)
                .quantity(transfer.getQuantity())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
