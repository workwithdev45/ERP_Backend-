package com.msmeerp.inventory.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.inventory.dto.InventoryAdjustmentRequest;
import com.msmeerp.inventory.dto.InventoryItemDto;
import com.msmeerp.inventory.dto.ProductDto;
import com.msmeerp.inventory.dto.StockMovementDto;
import com.msmeerp.inventory.dto.StockReservationRequest;
import com.msmeerp.inventory.dto.StockTransferRequest;
import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<Product>> createProduct(@Valid @RequestBody ProductDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createProduct(request), "Product created successfully"));
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllProducts()));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getProductById(id)));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<Product>> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateProduct(id, request), "Product updated successfully"));
    }

    @GetMapping("/products/{id}/stock")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<List<InventoryItemDto>>> getStockForProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getStockForProduct(id)));
    }

    @GetMapping("/products/{id}/movements")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getMovementsForProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getMovementsForProduct(id)));
    }

    @PostMapping("/warehouses")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<Warehouse>> createWarehouse(@Valid @RequestBody WarehouseDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createWarehouse(request), "Warehouse created successfully"));
    }

    @GetMapping("/warehouses")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<List<Warehouse>>> getAllWarehouses() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllWarehouses()));
    }

    @PutMapping("/warehouses/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<Warehouse>> updateWarehouse(@PathVariable Long id, @Valid @RequestBody WarehouseDto request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateWarehouse(id, request), "Warehouse updated successfully"));
    }

    @GetMapping("/stock")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<List<InventoryItemDto>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllInventory()));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<List<InventoryItemDto>>> getLowStockItems() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockItems()));
    }

    @PostMapping("/stock/adjust")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<InventoryItemDto>> adjustStock(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.adjustStock(request), "Stock updated successfully"));
    }

    @PostMapping("/stock/transfer")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<Void>> transferStock(@Valid @RequestBody StockTransferRequest request) {
        inventoryService.transferStock(request);
        return ResponseEntity.ok(ApiResponse.success("Stock transferred successfully"));
    }

    @PostMapping("/stock/reserve")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<InventoryItemDto>> reserveStock(@Valid @RequestBody StockReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.reserveStock(request), "Stock reserved"));
    }

    @PostMapping("/stock/release")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<InventoryItemDto>> releaseStock(@Valid @RequestBody StockReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.releaseStock(request), "Reservation released"));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getRecentMovements() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getRecentMovements()));
    }
}
