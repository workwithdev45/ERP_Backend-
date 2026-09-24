package com.msmeerp.inventory.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.dto.WarehouseUpsertRequest;
import com.msmeerp.inventory.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<WarehouseDto>> createWarehouse(@Valid @RequestBody WarehouseUpsertRequest request) {
        WarehouseDto response = warehouseService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Warehouse created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<WarehouseDto>> getWarehouseById(@PathVariable Long id) {
        WarehouseDto response = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<List<WarehouseDto>>> getAllWarehouses() {
        List<WarehouseDto> response = warehouseService.getAllWarehouses();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<WarehouseDto>> updateWarehouse(
            @PathVariable Long id, @Valid @RequestBody WarehouseUpsertRequest request) {
        WarehouseDto response = warehouseService.updateWarehouse(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Warehouse updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Warehouse deleted successfully"));
    }
}
