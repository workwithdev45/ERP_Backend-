package com.msmeerp.trade.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.trade.dto.AgeingPartyDto;
import com.msmeerp.trade.dto.ConvertQuotationRequest;
import com.msmeerp.trade.dto.DocumentDto;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.dto.DocumentSummaryDto;
import com.msmeerp.trade.dto.PaymentDto;
import com.msmeerp.trade.dto.PaymentRequest;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    // Module grants (SALES_VIEW/CREATE/EDIT/APPROVE) plus the older role permissions (SALES_READ/WRITE).
    private static final String CAN_VIEW = "hasRole('ADMIN') or hasAnyAuthority('SALES_VIEW', 'SALES_READ', 'SALES_WRITE')";
    private static final String CAN_CREATE = "hasRole('ADMIN') or hasAnyAuthority('SALES_CREATE', 'SALES_WRITE')";
    private static final String CAN_EDIT = "hasRole('ADMIN') or hasAnyAuthority('SALES_EDIT', 'SALES_WRITE')";

    private final SalesService salesService;

    @GetMapping("/documents/{id}")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<DocumentDto>> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(salesService.getDocument(id)));
    }

    @GetMapping("/quotations")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listQuotations() {
        return list(DocumentType.QUOTATION);
    }

    @PostMapping("/quotations")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createQuotation(@Valid @RequestBody DocumentRequest request) {
        return created(salesService.createQuotation(request), "Quotation created");
    }

    @PostMapping("/quotations/{id}/convert")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> convertQuotation(@PathVariable Long id, @Valid @RequestBody ConvertQuotationRequest request) {
        return created(salesService.convertQuotation(id, request), "Sales order created");
    }

    @PostMapping("/quotations/{id}/cancel")
    @PreAuthorize(CAN_EDIT)
    public ResponseEntity<ApiResponse<DocumentDto>> cancelQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(salesService.cancelQuotation(id), "Quotation cancelled"));
    }

    @GetMapping("/orders")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listOrders() {
        return list(DocumentType.SALES_ORDER);
    }

    @PostMapping("/orders")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createOrder(@Valid @RequestBody DocumentRequest request) {
        return created(salesService.createSalesOrder(request), "Sales order created");
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize(CAN_EDIT)
    public ResponseEntity<ApiResponse<DocumentDto>> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(salesService.cancelSalesOrder(id), "Sales order cancelled"));
    }

    @GetMapping("/deliveries")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listDeliveries() {
        return list(DocumentType.DELIVERY_CHALLAN);
    }

    @PostMapping("/deliveries")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createDelivery(@Valid @RequestBody DocumentRequest request) {
        return created(salesService.createDeliveryChallan(request), "Delivery challan created");
    }

    @GetMapping("/invoices")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listInvoices() {
        return list(DocumentType.SALES_INVOICE);
    }

    @PostMapping("/invoices")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createInvoice(@Valid @RequestBody DocumentRequest request) {
        return created(salesService.createInvoice(request), "Invoice created");
    }

    @GetMapping("/credit-notes")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listCreditNotes() {
        return list(DocumentType.CREDIT_NOTE);
    }

    @PostMapping("/credit-notes")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createCreditNote(@Valid @RequestBody DocumentRequest request) {
        return created(salesService.createCreditNote(request), "Credit note created");
    }

    @GetMapping("/receipts")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<PaymentDto>>> listReceipts() {
        return ResponseEntity.ok(ApiResponse.success(salesService.listReceipts()));
    }

    @PostMapping("/receipts")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<PaymentDto>> recordReceipt(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(salesService.recordReceipt(request), "Receipt recorded"));
    }

    @GetMapping("/receivables-ageing")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<AgeingPartyDto>>> receivablesAgeing() {
        return ResponseEntity.ok(ApiResponse.success(salesService.receivablesAgeing()));
    }

    private ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> list(DocumentType type) {
        return ResponseEntity.ok(ApiResponse.success(salesService.listDocuments(type)));
    }

    private static ResponseEntity<ApiResponse<DocumentDto>> created(DocumentDto document, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(document, message + " — " + document.getDocNumber()));
    }
}
