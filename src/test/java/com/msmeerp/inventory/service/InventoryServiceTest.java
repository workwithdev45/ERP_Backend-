package com.msmeerp.inventory.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.StockReservationRequest;
import com.msmeerp.inventory.dto.StockTransferRequest;
import com.msmeerp.inventory.entity.InventoryItem;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.InventoryItemRepository;
import com.msmeerp.inventory.repository.ProductRepository;
import com.msmeerp.inventory.repository.StockMovementRepository;
import com.msmeerp.inventory.repository.WarehouseRepository;
import com.msmeerp.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final String TENANT_ID = "tenant-1";

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

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private Product product(long id) {
        Product product = Product.builder().sku("SKU-001").name("Laptop").reorderLevel(0).build();
        product.setId(id);
        return product;
    }

    private Warehouse warehouse(long id) {
        Warehouse warehouse = Warehouse.builder().name("Main Warehouse").code("WH-01").build();
        warehouse.setId(id);
        return warehouse;
    }

    @Test
    void adjustStock_shouldIncreaseAvailableQuantityAndCreateMovement() {
        Product product = product(1L);
        Warehouse warehouse = warehouse(5L);
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse).availableQuantity(20).reservedQuantity(0).build();
        item.setId(10L);

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByTenantIdAndId(TENANT_ID, 5L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(TENANT_ID, 1L, 5L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(1L);
        request.setWarehouseId(5L);
        request.setQuantity(15);
        request.setReason("Stock receipt");

        InventoryItemDto result = inventoryService.adjustStock(request);

        assertThat(result.getAvailableQuantity()).isEqualTo(35);
        assertThat(result.getAvailableToPromise()).isEqualTo(35);
        verify(stockMovementRepository).save(any());
    }

    @Test
    void adjustStock_shouldRejectNegativeStockForOutwardMovement() {
        Product product = product(1L);
        Warehouse warehouse = warehouse(5L);
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse).availableQuantity(5).reservedQuantity(0).build();
        item.setId(10L);

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByTenantIdAndId(TENANT_ID, 5L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(TENANT_ID, 1L, 5L)).thenReturn(Optional.of(item));

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(1L);
        request.setWarehouseId(5L);
        request.setQuantity(-10);
        request.setReason("Stock issue");

        assertThatThrownBy(() -> inventoryService.adjustStock(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot result in negative stock");
    }

    @Test
    void adjustStock_shouldRejectIssuingReservedStock() {
        Product product = product(1L);
        Warehouse warehouse = warehouse(5L);
        // 10 on hand, all 10 reserved -> nothing left to promise
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse).availableQuantity(10).reservedQuantity(10).build();
        item.setId(10L);

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByTenantIdAndId(TENANT_ID, 5L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(TENANT_ID, 1L, 5L)).thenReturn(Optional.of(item));

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(1L);
        request.setWarehouseId(5L);
        request.setQuantity(-1);

        assertThatThrownBy(() -> inventoryService.adjustStock(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void adjustStock_shouldRecomputeWeightedAverageCostOnReceipt() {
        Product product = product(1L);
        Warehouse warehouse = warehouse(5L);
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse)
                .availableQuantity(10).reservedQuantity(0).averageCost(new BigDecimal("100.0000")).build();
        item.setId(10L);

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByTenantIdAndId(TENANT_ID, 5L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(TENANT_ID, 1L, 5L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(1L);
        request.setWarehouseId(5L);
        request.setQuantity(10);
        request.setUnitCost(new BigDecimal("200"));

        InventoryItemDto result = inventoryService.adjustStock(request);

        // (10 * 100 + 10 * 200) / 20 = 150
        assertThat(result.getAverageCost()).isEqualByComparingTo("150.0000");
    }

    @Test
    void reserveStock_shouldRejectReservingMoreThanAvailableToPromise() {
        Product product = product(1L);
        Warehouse warehouse = warehouse(5L);
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse).availableQuantity(5).reservedQuantity(2).build();
        item.setId(10L);

        when(inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(TENANT_ID, 1L, 5L)).thenReturn(Optional.of(item));

        StockReservationRequest request = StockReservationRequest.builder().productId(1L).warehouseId(5L).quantity(4).build();

        assertThatThrownBy(() -> inventoryService.reserveStock(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void transferStock_shouldRejectSameSourceAndDestination() {
        StockTransferRequest request = StockTransferRequest.builder()
                .productId(1L).fromWarehouseId(5L).toWarehouseId(5L).quantity(1).build();

        assertThatThrownBy(() -> inventoryService.transferStock(request)).isInstanceOf(BadRequestException.class);
    }
}
