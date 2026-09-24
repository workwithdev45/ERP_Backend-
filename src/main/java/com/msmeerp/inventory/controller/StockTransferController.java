package com.msmeerp.inventory.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.common.util.AppConstants;
import com.msmeerp.inventory.dto.StockTransferDto;
import com.msmeerp.inventory.dto.StockTransferRequest;
import com.msmeerp.inventory.service.StockTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-transfers")
@RequiredArgsConstructor
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockTransferDto>> createTransfer(@Valid @RequestBody StockTransferRequest request) {
        StockTransferDto response = stockTransferService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock transfer created successfully"));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockTransferDto>> completeTransfer(@PathVariable Long id) {
        StockTransferDto response = stockTransferService.completeTransfer(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock transfer completed successfully"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<ApiResponse<StockTransferDto>> cancelTransfer(@PathVariable Long id) {
        StockTransferDto response = stockTransferService.cancelTransfer(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock transfer cancelled successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
    public ResponseEntity<ApiResponse<PagedResponse<StockTransferDto>>> getAllTransfers(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<StockTransferDto> response = stockTransferService.getAllTransfers(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
