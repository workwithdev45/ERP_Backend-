package com.msmeerp.reports.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.reports.dto.AnalyticsSummaryDto;
import com.msmeerp.reports.dto.RegisterDto;
import com.msmeerp.reports.dto.StockValuationDto;
import com.msmeerp.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    // Module grant REPORTS_VIEW plus the older role permission REPORTS_READ.
    private static final String CAN_VIEW = "hasRole('ADMIN') or hasAnyAuthority('REPORTS_VIEW', 'REPORTS_READ')";

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<AnalyticsSummaryDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(reportService.dashboard()));
    }

    @GetMapping("/sales-register")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<RegisterDto>> salesRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.salesRegister(from, to)));
    }

    @GetMapping("/purchase-register")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<RegisterDto>> purchaseRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.purchaseRegister(from, to)));
    }

    @GetMapping("/stock-valuation")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<StockValuationDto>> stockValuation() {
        return ResponseEntity.ok(ApiResponse.success(reportService.stockValuation()));
    }
}
