package com.msmeerp.inventory.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.common.util.AppConstants;
import com.msmeerp.inventory.dto.StockItemDto;
import com.msmeerp.inventory.dto.StockItemUpsertRequest;
import com.msmeerp.inventory.service.StockItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/stock-items")
@RequiredArgsConstructor
public class StockItemController {

    private final StockItemService stockItemService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockItemDto>> createItem(@Valid @RequestBody StockItemUpsertRequest request) {
        StockItemDto response = stockItemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock item created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<StockItemDto>> getItemById(@PathVariable Long id) {
        StockItemDto response = stockItemService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<PagedResponse<StockItemDto>>> getAllItems(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<StockItemDto> response = stockItemService.getAllItems(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<List<StockItemDto>>> getLowStockItems() {
        List<StockItemDto> response = stockItemService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockItemDto>> updateItem(
            @PathVariable Long id, @Valid @RequestBody StockItemUpsertRequest request) {
        StockItemDto response = stockItemService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock item updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        stockItemService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock item deleted successfully"));
    }
}
