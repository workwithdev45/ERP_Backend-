package com.msmeerp.inventory.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.ProductDto;
import com.msmeerp.inventory.dto.StockMovementDto;
import com.msmeerp.inventory.dto.StockReservationRequest;
import com.msmeerp.inventory.dto.StockTransferRequest;
import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.entity.InventoryItem;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.InventoryItemRepository;
import com.msmeerp.inventory.repository.ProductRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.repository.WarehouseRepository;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public Product createProduct(ProductDto request) {
        String tenantId = TenantContext.getTenantId();
        if (productRepository.findByTenantIdAndSku(tenantId, request.getSku()).isPresent()) {
            throw new BadRequestException("A product with SKU '" + request.getSku() + "' already exists");
        }
        if (StringUtils.hasText(request.getBarcode())
                && productRepository.findByTenantIdAndBarcode(tenantId, request.getBarcode()).isPresent()) {
            throw new BadRequestException("That barcode is already assigned to another product");
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .itemType(request.getItemType() == null ? com.msmeerp.inventory.entity.ItemType.STOCK : request.getItemType())
                .hsnCode(request.getHsnCode())
                .gstRatePercent(request.getGstRatePercent())
                .barcode(request.getBarcode())
                .imageUrl(request.getImageUrl())
                .unitOfMeasure(request.getUnitOfMeasure())
                .secondaryUnit(request.getSecondaryUnit())
                .conversionFactor(request.getConversionFactor())
                .reorderLevel(request.getReorderLevel() == null ? 0 : request.getReorderLevel())
                .active(request.getActive() == null || request.getActive())
                .build();
        product.setTenantId(tenantId);
        Product saved = productRepository.save(product);

        if (request.getOpeningStockWarehouseId() != null
                && request.getOpeningStockQuantity() != null
                && request.getOpeningStockQuantity() > 0) {
            Warehouse warehouse = warehouseRepository.findByTenantIdAndId(tenantId, request.getOpeningStockWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getOpeningStockWarehouseId()));
            receiveStock(tenantId, saved, warehouse, request.getOpeningStockQuantity(),
                    request.getOpeningStockUnitCost(), StockMovement.MovementType.OPENING, "OPENING", null, "Opening stock");
        }

        return saved;
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductDto request) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        if (request.getItemType() != null) {
            product.setItemType(request.getItemType());
        }
        product.setHsnCode(request.getHsnCode());
        product.setGstRatePercent(request.getGstRatePercent());
        product.setBarcode(request.getBarcode());
        product.setImageUrl(request.getImageUrl());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setSecondaryUnit(request.getSecondaryUnit());
        product.setConversionFactor(request.getConversionFactor());
        product.setReorderLevel(request.getReorderLevel() == null ? product.getReorderLevel() : request.getReorderLevel());
        product.setActive(request.getActive() == null ? product.getActive() : request.getActive());
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        String tenantId = TenantContext.getTenantId();
        return productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findByTenantIdOrderByNameAsc(TenantContext.getTenantId());
    }

    @Override
    @Transactional
    public Warehouse createWarehouse(WarehouseDto request) {
        String tenantId = TenantContext.getTenantId();
        if (warehouseRepository.findByTenantIdAndCode(tenantId, request.getCode()).isPresent()) {
            throw new BadRequestException("A warehouse with code '" + request.getCode() + "' already exists");
        }
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .code(request.getCode())
                .location(request.getLocation())
                .defaultWarehouse(request.getDefaultWarehouse() != null && request.getDefaultWarehouse())
                .build();
        warehouse.setTenantId(tenantId);
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public Warehouse updateWarehouse(Long id, WarehouseDto request) {
        String tenantId = TenantContext.getTenantId();
        Warehouse warehouse = warehouseRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        warehouse.setName(request.getName());
        warehouse.setCode(request.getCode());
        warehouse.setLocation(request.getLocation());
        warehouse.setDefaultWarehouse(request.getDefaultWarehouse() != null && request.getDefaultWarehouse());
        return warehouseRepository.save(warehouse);
    }

    @Override
    public Warehouse getWarehouseById(Long id) {
        String tenantId = TenantContext.getTenantId();
        return warehouseRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
    }

    @Override
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findByTenantIdOrderByNameAsc(TenantContext.getTenantId());
    }

    @Override
    @Transactional
    public InventoryItemDto adjustStock(InventoryAdjustmentRequest request) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        Warehouse warehouse = warehouseRepository.findByTenantIdAndId(tenantId, request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        StockMovement.MovementType type = request.getQuantity() >= 0 ? StockMovement.MovementType.IN : StockMovement.MovementType.OUT;
        if (request.getReasonCode() != null) {
            type = StockMovement.MovementType.ADJUSTMENT;
        }

        InventoryItem item;
        if (request.getQuantity() >= 0) {
            item = receiveStock(tenantId, product, warehouse, request.getQuantity(), request.getUnitCost(),
                    type, "ADJUSTMENT", request.getReasonCode(), request.getReason());
        } else {
            item = issueStock(tenantId, product, warehouse, -request.getQuantity(),
                    type, "ADJUSTMENT", request.getReasonCode(), request.getReason());
        }

        return mapItem(item);
    }

    @Override
    public List<InventoryItemDto> getAllInventory() {
        return inventoryItemRepository.findAllWithDetails(TenantContext.getTenantId()).stream()
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemDto> getStockForProduct(Long productId) {
        return inventoryItemRepository.findByProductId(TenantContext.getTenantId(), productId).stream()
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemDto> getLowStockItems() {
        return inventoryItemRepository.findLowStockItems(TenantContext.getTenantId()).stream()
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDto> getRecentMovements() {
        return stockMovementRepository.findByTenantIdOrderByPerformedAtDesc(TenantContext.getTenantId()).stream()
                .map(this::mapMovement)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDto> getMovementsForProduct(Long productId) {
        return stockMovementRepository.findByTenantIdAndProductIdOrderByPerformedAtDesc(TenantContext.getTenantId(), productId).stream()
                .map(this::mapMovement)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void transferStock(StockTransferRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BadRequestException("Source and destination warehouse must be different");
        }
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        Warehouse fromWarehouse = warehouseRepository.findByTenantIdAndId(tenantId, request.getFromWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getFromWarehouseId()));
        Warehouse toWarehouse = warehouseRepository.findByTenantIdAndId(tenantId, request.getToWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getToWarehouseId()));

        InventoryItem source = issueStock(tenantId, product, fromWarehouse, request.getQuantity(),
                StockMovement.MovementType.TRANSFER, "TRANSFER_OUT", null, request.getReason());

        // The stock carries its cost with it, so the destination's weighted average blends in at the source's cost.
        receiveStock(tenantId, product, toWarehouse, request.getQuantity(), source.getAverageCost(),
                StockMovement.MovementType.TRANSFER, "TRANSFER_IN", null, request.getReason());
    }

    @Override
    @Transactional
    public InventoryItemDto reserveStock(StockReservationRequest request) {
        String tenantId = TenantContext.getTenantId();
        InventoryItem item = inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(
                        tenantId, request.getProductId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("No stock found for this product in that warehouse"));

        int availableToPromise = item.getAvailableQuantity() - item.getReservedQuantity();
        if (request.getQuantity() > availableToPromise) {
            throw new BadRequestException("Only " + availableToPromise + " unit(s) available to reserve");
        }
        item.setReservedQuantity(item.getReservedQuantity() + request.getQuantity());
        return mapItem(inventoryItemRepository.save(item));
    }

    @Override
    @Transactional
    public InventoryItemDto releaseStock(StockReservationRequest request) {
        String tenantId = TenantContext.getTenantId();
        InventoryItem item = inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(
                        tenantId, request.getProductId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("No stock found for this product in that warehouse"));

        item.setReservedQuantity(Math.max(0, item.getReservedQuantity() - request.getQuantity()));
        return mapItem(inventoryItemRepository.save(item));
    }

    // -- internal ledger helpers -------------------------------------------------------------

    private InventoryItem findOrCreateItem(String tenantId, Product product, Warehouse warehouse) {
        return inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(tenantId, product.getId(), warehouse.getId())
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setWarehouse(warehouse);
                    newItem.setAvailableQuantity(0);
                    newItem.setReservedQuantity(0);
                    newItem.setTenantId(tenantId);
                    return newItem;
                });
    }

    /** Adds stock and, if a cost was given, folds it into the item's weighted-average cost. */
    private InventoryItem receiveStock(String tenantId, Product product, Warehouse warehouse, int quantity,
                                        BigDecimal unitCost, StockMovement.MovementType type, String referenceType,
                                        StockMovement.AdjustmentReason reasonCode, String reason) {
        InventoryItem item = findOrCreateItem(tenantId, product, warehouse);

        if (unitCost != null) {
            BigDecimal existingQty = BigDecimal.valueOf(item.getAvailableQuantity());
            BigDecimal existingValue = existingQty.multiply(item.getAverageCost() == null ? BigDecimal.ZERO : item.getAverageCost());
            BigDecimal incomingValue = BigDecimal.valueOf(quantity).multiply(unitCost);
            BigDecimal totalQty = existingQty.add(BigDecimal.valueOf(quantity));
            item.setAverageCost(totalQty.signum() == 0
                    ? BigDecimal.ZERO
                    : existingValue.add(incomingValue).divide(totalQty, 4, RoundingMode.HALF_UP));
        }

        item.setAvailableQuantity(item.getAvailableQuantity() + quantity);
        InventoryItem saved = inventoryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .movementType(type)
                .quantity(quantity)
                .unitCost(unitCost)
                .totalValue(unitCost == null ? null : unitCost.multiply(BigDecimal.valueOf(quantity)))
                .reasonCode(reasonCode)
                .referenceType(referenceType)
                .referenceId(referenceType + "-" + System.currentTimeMillis())
                .reason(reason)
                .build();
        movement.setTenantId(tenantId);
        stockMovementRepository.save(movement);

        return saved;
    }

    /** Removes stock, enforcing the negative-stock rule against what's still available to promise. */
    private InventoryItem issueStock(String tenantId, Product product, Warehouse warehouse, int quantity,
                                      StockMovement.MovementType type, String referenceType,
                                      StockMovement.AdjustmentReason reasonCode, String reason) {
        InventoryItem item = inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(tenantId, product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No stock found for this product in that warehouse"));

        int availableToPromise = item.getAvailableQuantity() - item.getReservedQuantity();
        if (quantity > availableToPromise) {
            throw new BadRequestException(
                    "Stock adjustment cannot result in negative stock (only " + availableToPromise + " available)");
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        InventoryItem saved = inventoryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .movementType(type)
                .quantity(quantity)
                .reasonCode(reasonCode)
                .referenceType(referenceType)
                .referenceId(referenceType + "-" + System.currentTimeMillis())
                .reason(reason)
                .build();
        movement.setTenantId(tenantId);
        stockMovementRepository.save(movement);

        return saved;
    }

    private InventoryItemDto mapItem(InventoryItem item) {
        boolean lowStock = item.getAvailableQuantity() <= item.getProduct().getReorderLevel();
        return InventoryItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .sku(item.getProduct().getSku())
                .warehouseId(item.getWarehouse().getId())
                .warehouseName(item.getWarehouse().getName())
                .availableQuantity(item.getAvailableQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .availableToPromise(item.getAvailableQuantity() - item.getReservedQuantity())
                .averageCost(item.getAverageCost())
                .reorderLevel(item.getProduct().getReorderLevel())
                .lowStock(lowStock)
                .build();
    }

    private StockMovementDto mapMovement(StockMovement movement) {
        return StockMovementDto.builder()
                .id(movement.getId())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .sku(movement.getProduct().getSku())
                .warehouseId(movement.getWarehouse().getId())
                .warehouseName(movement.getWarehouse().getName())
                .movementType(movement.getMovementType().name())
                .quantity(movement.getQuantity())
                .unitCost(movement.getUnitCost())
                .totalValue(movement.getTotalValue())
                .reasonCode(movement.getReasonCode() == null ? null : movement.getReasonCode().name())
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .reason(movement.getReason())
                .performedAt(movement.getPerformedAt())
                .build();
    }
}
