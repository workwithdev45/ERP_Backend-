package com.msmeerp.inventory.service;

import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.ProductDto;
import com.msmeerp.inventory.dto.StockMovementDto;
import com.msmeerp.inventory.dto.StockReservationRequest;
import com.msmeerp.inventory.dto.StockTransferRequest;
import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.entity.Warehouse;

import java.math.BigDecimal;

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

    // -- hooks for Purchase and Sales documents -------------------------------------------------

    /** Stock-in posted by another module's document (GRN, direct bill, sales return). */
    void receiveForDocument(Long productId, Long warehouseId, int quantity, BigDecimal unitCost,
                            StockMovement.MovementType type, String referenceType, String referenceId, String reason);

    /** Stock-out posted by another module's document (delivery, direct invoice, purchase return). */
    void issueForDocument(Long productId, Long warehouseId, int quantity,
                          StockMovement.MovementType type, String referenceType, String referenceId, String reason);

    /** Reserves as much of {@code quantity} as is available to promise; returns how much was reserved. */
    int reserveAvailable(Long productId, Long warehouseId, int quantity);

    /** Releases up to {@code quantity} previously reserved units. */
    void releaseReserved(Long productId, Long warehouseId, int quantity);
}
