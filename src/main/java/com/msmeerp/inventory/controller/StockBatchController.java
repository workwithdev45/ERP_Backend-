package com.msmeerp.inventory.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.common.util.AppConstants;
import com.msmeerp.inventory.dto.StockBatchDto;
import com.msmeerp.inventory.dto.StockBatchUpsertRequest;
import com.msmeerp.inventory.service.StockBatchService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock-batches")
@RequiredArgsConstructor
public class StockBatchController {

    private final StockBatchService stockBatchService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockBatchDto>> createBatch(@Valid @RequestBody StockBatchUpsertRequest request) {
        StockBatchDto response = stockBatchService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock batch created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<StockBatchDto>> getBatchById(@PathVariable Long id) {
        StockBatchDto response = stockBatchService.getBatchById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<List<StockBatchDto>>> getAllBatches(
            @RequestParam(required = false) Long itemId) {
        List<StockBatchDto> response = itemId != null
                ? stockBatchService.getBatchesForItem(itemId)
                : stockBatchService.getAllBatches();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/expiring-soon")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<List<StockBatchDto>>> getExpiringSoon(
            @RequestParam(defaultValue = AppConstants.DEFAULT_EXPIRING_SOON_DAYS) int days) {
        List<StockBatchDto> response = stockBatchService.getExpiringSoon(days);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockBatchDto>> updateBatch(
            @PathVariable Long id, @Valid @RequestBody StockBatchUpsertRequest request) {
        StockBatchDto response = stockBatchService.updateBatch(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock batch updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@PathVariable Long id) {
        stockBatchService.deleteBatch(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock batch deleted successfully"));
    }
}
