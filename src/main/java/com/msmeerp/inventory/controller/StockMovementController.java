package com.msmeerp.inventory.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.common.util.AppConstants;
import com.msmeerp.inventory.dto.RecordMovementRequest;
import com.msmeerp.inventory.dto.StockMovementDto;
import com.msmeerp.inventory.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockMovementDto>> recordMovement(@Valid @RequestBody RecordMovementRequest request) {
        StockMovementDto response = stockMovementService.recordMovement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock movement recorded successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<PagedResponse<StockMovementDto>>> getMovements(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<StockMovementDto> response = stockMovementService.getMovements(itemId, warehouseId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
