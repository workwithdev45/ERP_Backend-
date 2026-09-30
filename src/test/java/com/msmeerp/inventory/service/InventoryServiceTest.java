package com.msmeerp.inventory.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.ProductDto;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    void adjustStock_shouldRecordOutwardAdjustmentAsNegativeQuantity() {
        Product product = product(1L);
        Warehouse warehouse = warehouse(5L);
        InventoryItem item = InventoryItem.builder().product(product).warehouse(warehouse).availableQuantity(10).reservedQuantity(0).build();
        item.setId(10L);

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByTenantIdAndId(TENANT_ID, 5L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByTenantIdAndProductIdAndWarehouseId(TENANT_ID, 1L, 5L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(1L);
        request.setWarehouseId(5L);
        request.setQuantity(-3);
        request.setReasonCode(StockMovement.AdjustmentReason.DAMAGE);

        inventoryService.adjustStock(request);

        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getMovementType()).isEqualTo(StockMovement.MovementType.ADJUSTMENT);
        assertThat(movement.getValue().getQuantity()).isEqualTo(-3);
    }

    @Test
    void updateProduct_shouldRejectSkuUsedByAnotherProduct() {
        Product existing = product(1L);
        Product other = product(2L);
        other.setSku("SKU-002");

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(existing));
        when(productRepository.findByTenantIdAndSku(TENANT_ID, "SKU-002")).thenReturn(Optional.of(other));

        ProductDto request = ProductDto.builder().sku("SKU-002").name("Laptop").build();

        assertThatThrownBy(() -> inventoryService.updateProduct(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SKU-002");
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_shouldAllowKeepingItsOwnSku() {
        Product existing = product(1L);

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(existing));
        when(productRepository.findByTenantIdAndSku(TENANT_ID, "SKU-001")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto request = ProductDto.builder().sku("SKU-001").name("Laptop Pro").build();

        assertThat(inventoryService.updateProduct(1L, request).getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void updateWarehouse_shouldRejectCodeUsedByAnotherWarehouse() {
        Warehouse existing = warehouse(5L);
        Warehouse other = warehouse(6L);
        other.setCode("WH-02");

        when(warehouseRepository.findByTenantIdAndId(TENANT_ID, 5L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.findByTenantIdAndCode(TENANT_ID, "WH-02")).thenReturn(Optional.of(other));

        WarehouseDto request = WarehouseDto.builder().name("Main Warehouse").code("WH-02").build();

        assertThatThrownBy(() -> inventoryService.updateWarehouse(5L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("WH-02");
    }

    @Test
    void updateWarehouse_shouldUnsetPreviousDefaultWhenMarkingNewDefault() {
        Warehouse target = warehouse(5L);
        Warehouse previousDefault = warehouse(6L);
        previousDefault.setDefaultWarehouse(true);

        when(warehouseRepository.findByTenantIdAndId(TENANT_ID, 5L)).thenReturn(Optional.of(target));
        when(warehouseRepository.findByTenantIdAndCode(TENANT_ID, "WH-01")).thenReturn(Optional.of(target));
        when(warehouseRepository.findByTenantIdAndDefaultWarehouseTrue(TENANT_ID)).thenReturn(List.of(previousDefault));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseDto request = WarehouseDto.builder().name("Main Warehouse").code("WH-01").defaultWarehouse(true).build();

        Warehouse result = inventoryService.updateWarehouse(5L, request);

        assertThat(result.getDefaultWarehouse()).isTrue();
        assertThat(previousDefault.getDefaultWarehouse()).isFalse();
    }

    @Test
    void transferStock_shouldRejectSameSourceAndDestination() {
        StockTransferRequest request = StockTransferRequest.builder()
                .productId(1L).fromWarehouseId(5L).toWarehouseId(5L).quantity(1).build();

        assertThatThrownBy(() -> inventoryService.transferStock(request)).isInstanceOf(BadRequestException.class);
    }
}
