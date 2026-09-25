package com.msmeerp.inventory.service;

import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.entity.InventoryItem;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.InventoryItemRepository;
import com.msmeerp.inventory.repository.ProductRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void adjustStock_shouldIncreaseAvailableQuantityAndCreateMovement() {
        Product product = Product.builder().sku("SKU-001").name("Laptop").build();
        product.setId(1L);
        Warehouse warehouse = Warehouse.builder().name("Main Warehouse").code("WH-01").build();
        warehouse.setId(5L);
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse).availableQuantity(20).build();
        item.setId(10L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(5L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByProductIdAndWarehouseId(1L, 5L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(1L);
        request.setWarehouseId(5L);
        request.setQuantity(15);
        request.setReason("Stock receipt");

        InventoryItemDto result = inventoryService.adjustStock(request);

        assertThat(result.getAvailableQuantity()).isEqualTo(35);
        verify(stockMovementRepository).save(any());
    }

    @Test
    void adjustStock_shouldRejectNegativeStockForOutwardMovement() {
        Product product = Product.builder().sku("SKU-001").name("Laptop").build();
        product.setId(1L);
        Warehouse warehouse = Warehouse.builder().name("Main Warehouse").code("WH-01").build();
        warehouse.setId(5L);
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse).availableQuantity(5).build();
        item.setId(10L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(5L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByProductIdAndWarehouseId(1L, 5L)).thenReturn(Optional.of(item));

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(1L);
        request.setWarehouseId(5L);
        request.setQuantity(-10);
        request.setReason("Stock issue");

        assertThatThrownBy(() -> inventoryService.adjustStock(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot result in negative stock");
    }
}
