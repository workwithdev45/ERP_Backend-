package com.msmeerp.inventory.service;

import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.ProductDto;
import com.msmeerp.inventory.dto.StockMovementDto;
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
    List<InventoryItemDto> getLowStockItems();
    List<StockMovementDto> getRecentMovements();
}
