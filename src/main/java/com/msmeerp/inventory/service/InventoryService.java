package com.msmeerp.inventory.service;

import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.ProductDto;
import com.msmeerp.inventory.dto.StockMovementDto;
import com.msmeerp.inventory.dto.StockReservationRequest;
import com.msmeerp.inventory.dto.StockTransferRequest;
import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.Warehouse;

import java.util.List;

public interface InventoryService {
    Product createProduct(ProductDto request);
    Product updateProduct(Long id, ProductDto request);
    Product getProductById(Long id);
    List<Product> getAllProducts();

    Warehouse createWarehouse(WarehouseDto request);
    Warehouse updateWarehouse(Long id, WarehouseDto request);
    Warehouse getWarehouseById(Long id);
    List<Warehouse> getAllWarehouses();

    InventoryItemDto adjustStock(InventoryAdjustmentRequest request);
    List<InventoryItemDto> getAllInventory();
    List<InventoryItemDto> getStockForProduct(Long productId);
    List<InventoryItemDto> getLowStockItems();
    List<StockMovementDto> getRecentMovements();
    List<StockMovementDto> getMovementsForProduct(Long productId);

    /** W8: move stock between two warehouses as one atomic OUT + IN. */
    void transferStock(StockTransferRequest request);

    /** W8: commit stock to a pending order without moving it — reduces available-to-promise. */
    InventoryItemDto reserveStock(StockReservationRequest request);

    /** Releases a reservation made by {@link #reserveStock}, e.g. when an order is cancelled. */
    InventoryItemDto releaseStock(StockReservationRequest request);
}
