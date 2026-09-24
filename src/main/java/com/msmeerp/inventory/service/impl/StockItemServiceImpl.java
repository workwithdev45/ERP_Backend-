package com.msmeerp.inventory.service.impl;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.dto.StockItemDto;
import com.msmeerp.inventory.dto.StockItemUpsertRequest;
import com.msmeerp.inventory.entity.StockItem;
import com.msmeerp.inventory.repository.StockItemRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.service.StockItemService;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockItemServiceImpl implements StockItemService {

    private final StockItemRepository stockItemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public StockItemDto createItem(StockItemUpsertRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (stockItemRepository.existsByTenantIdAndSku(tenantId, request.getSku())) {
            throw new BadRequestException("SKU '" + request.getSku() + "' already exists for this tenant");
        }

        StockItem item = StockItem.builder()
                .sku(request.getSku())
                .name(request.getName())
                .category(request.getCategory())
                .uom(request.getUom())
                .reorderThreshold(request.getReorderThreshold())
                .active(request.getActive() == null || request.getActive())
                .build();
        item.setTenantId(tenantId);

        StockItem saved = stockItemRepository.save(item);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StockItemDto getItemById(Long id) {
        return mapToDto(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StockItemDto> getAllItems(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<StockItem> page = stockItemRepository.findByTenantId(tenantId, pageable);
        return PagedResponse.from(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItemDto> getLowStockItems() {
        String tenantId = TenantContext.getTenantId();
        return stockItemRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .filter(StockItemDto::isLowStock)
                .toList();
    }

    @Override
    @Transactional
    public StockItemDto updateItem(Long id, StockItemUpsertRequest request) {
        StockItem item = findById(id);

        String tenantId = TenantContext.getTenantId();
        if (!item.getSku().equalsIgnoreCase(request.getSku())
                && stockItemRepository.existsByTenantIdAndSku(tenantId, request.getSku())) {
            throw new BadRequestException("SKU '" + request.getSku() + "' already exists for this tenant");
        }

        item.setSku(request.getSku());
        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setUom(request.getUom());
        item.setReorderThreshold(request.getReorderThreshold());
        if (request.getActive() != null) {
            item.setActive(request.getActive());
        }

        return mapToDto(stockItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        StockItem item = findById(id);
        stockItemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal computeCurrentStock(Long itemId) {
        String tenantId = TenantContext.getTenantId();
        return stockMovementRepository.sumQuantityByStockItem(tenantId, itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal computeCurrentStock(Long itemId, Long warehouseId) {
        String tenantId = TenantContext.getTenantId();
        return stockMovementRepository.sumQuantityByStockItemAndWarehouse(tenantId, itemId, warehouseId);
    }

    private StockItem findById(Long id) {
        StockItem item = stockItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockItem", "id", id));

        String tenantId = TenantContext.getTenantId();
        if (item.getTenantId() != null && !item.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("StockItem", "id", id);
        }
        return item;
    }

    private StockItemDto mapToDto(StockItem item) {
        BigDecimal currentStock = computeCurrentStock(item.getId());
        boolean lowStock = currentStock.compareTo(item.getReorderThreshold()) < 0;

        return StockItemDto.builder()
                .id(item.getId())
                .sku(item.getSku())
                .name(item.getName())
                .category(item.getCategory())
                .uom(item.getUom())
                .reorderThreshold(item.getReorderThreshold())
                .active(item.isActive())
                .currentStock(currentStock)
                .lowStock(lowStock)
                .build();
    }
}
