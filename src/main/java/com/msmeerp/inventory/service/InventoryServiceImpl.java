package com.msmeerp.inventory.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.ProductDto;
import com.msmeerp.inventory.dto.StockMovementDto;
import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.entity.InventoryItem;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.InventoryItemRepository;
import com.msmeerp.inventory.repository.ProductRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new BadRequestException("Product SKU already exists");
        }
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .unitOfMeasure(request.getUnitOfMeasure())
                .reorderLevel(request.getReorderLevel() == null ? 0 : request.getReorderLevel())
                .active(request.getActive() == null || request.getActive())
                .build();
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setReorderLevel(request.getReorderLevel() == null ? product.getReorderLevel() : request.getReorderLevel());
        product.setActive(request.getActive() == null ? product.getActive() : request.getActive());
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional
    public Warehouse createWarehouse(WarehouseDto request) {
        if (warehouseRepository.findByCode(request.getCode()).isPresent()) {
            throw new BadRequestException("Warehouse code already exists");
        }
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .code(request.getCode())
                .location(request.getLocation())
                .defaultWarehouse(request.getDefaultWarehouse() != null && request.getDefaultWarehouse())
                .build();
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public Warehouse updateWarehouse(Long id, WarehouseDto request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        warehouse.setName(request.getName());
        warehouse.setCode(request.getCode());
        warehouse.setLocation(request.getLocation());
        warehouse.setDefaultWarehouse(request.getDefaultWarehouse() != null && request.getDefaultWarehouse());
        return warehouseRepository.save(warehouse);
    }

    @Override
    public Warehouse getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
    }

    @Override
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    @Transactional
    public InventoryItemDto adjustStock(InventoryAdjustmentRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        InventoryItem item = inventoryItemRepository.findByProductIdAndWarehouseId(request.getProductId(), request.getWarehouseId())
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setWarehouse(warehouse);
                    newItem.setAvailableQuantity(0);
                    newItem.setReservedQuantity(0);
                    return newItem;
                });

        int newQuantity = item.getAvailableQuantity() + request.getQuantity();
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock adjustment cannot result in negative stock");
        }

        item.setAvailableQuantity(newQuantity);
        InventoryItem savedItem = inventoryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .movementType(request.getQuantity() >= 0 ? StockMovement.MovementType.IN : StockMovement.MovementType.OUT)
                .quantity(Math.abs(request.getQuantity()))
                .reason(request.getReason())
                .referenceType("ADJUSTMENT")
                .referenceId("STOCK-ADJ-" + System.currentTimeMillis())
                .build();
        stockMovementRepository.save(movement);

        return mapItem(savedItem);
    }

    @Override
    public List<InventoryItemDto> getAllInventory() {
        return inventoryItemRepository.findAllWithDetails().stream()
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemDto> getLowStockItems() {
        return inventoryItemRepository.findLowStockItems().stream()
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDto> getRecentMovements() {
        return stockMovementRepository.findAllByOrderByPerformedAtDesc().stream()
                .map(movement -> StockMovementDto.builder()
                        .id(movement.getId())
                        .productId(movement.getProduct().getId())
                        .productName(movement.getProduct().getName())
                        .sku(movement.getProduct().getSku())
                        .warehouseId(movement.getWarehouse().getId())
                        .warehouseName(movement.getWarehouse().getName())
                        .movementType(movement.getMovementType().name())
                        .quantity(movement.getQuantity())
                        .referenceType(movement.getReferenceType())
                        .referenceId(movement.getReferenceId())
                        .reason(movement.getReason())
                        .performedAt(movement.getPerformedAt())
                        .build())
                .collect(Collectors.toList());
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
                .reorderLevel(item.getProduct().getReorderLevel())
                .lowStock(lowStock)
                .build();
    }
}
