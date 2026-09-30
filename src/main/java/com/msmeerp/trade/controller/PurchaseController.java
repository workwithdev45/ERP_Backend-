package com.msmeerp.trade.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.trade.dto.AgeingPartyDto;
import com.msmeerp.trade.dto.DocumentDto;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.dto.DocumentSummaryDto;
import com.msmeerp.trade.dto.PaymentDto;
import com.msmeerp.trade.dto.PaymentRequest;
import com.msmeerp.trade.dto.ReorderSuggestionDto;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.service.PurchaseService;
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
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    // Module grants (PURCHASE_VIEW/CREATE/EDIT/APPROVE) plus the older role permissions (PURCHASE_READ/WRITE).
    private static final String CAN_VIEW = "hasRole('ADMIN') or hasAnyAuthority('PURCHASE_VIEW', 'PURCHASE_READ', 'PURCHASE_WRITE')";
    private static final String CAN_CREATE = "hasRole('ADMIN') or hasAnyAuthority('PURCHASE_CREATE', 'PURCHASE_WRITE')";
    private static final String CAN_EDIT = "hasRole('ADMIN') or hasAnyAuthority('PURCHASE_EDIT', 'PURCHASE_WRITE')";
    private static final String CAN_APPROVE = "hasRole('ADMIN') or hasAuthority('PURCHASE_APPROVE')";

    private final PurchaseService purchaseService;

    @GetMapping("/documents/{id}")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<DocumentDto>> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.getDocument(id)));
    }

    @GetMapping("/orders")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listOrders() {
        return list(DocumentType.PURCHASE_ORDER);
    }

    @PostMapping("/orders")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createOrder(@Valid @RequestBody DocumentRequest request) {
        return created(purchaseService.createPurchaseOrder(request), "Purchase order created");
    }

    @PostMapping("/orders/{id}/approve")
    @PreAuthorize(CAN_APPROVE)
    public ResponseEntity<ApiResponse<DocumentDto>> approveOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.approvePurchaseOrder(id), "Purchase order approved"));
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize(CAN_EDIT)
    public ResponseEntity<ApiResponse<DocumentDto>> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.cancelPurchaseOrder(id), "Purchase order cancelled"));
    }

    @GetMapping("/receipts")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listReceipts() {
        return list(DocumentType.GOODS_RECEIPT);
    }

    @PostMapping("/receipts")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createReceipt(@Valid @RequestBody DocumentRequest request) {
        return created(purchaseService.createGoodsReceipt(request), "Goods received");
    }

    @GetMapping("/bills")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listBills() {
        return list(DocumentType.PURCHASE_BILL);
    }

    @PostMapping("/bills")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createBill(@Valid @RequestBody DocumentRequest request) {
        return created(purchaseService.createBill(request), "Bill recorded");
    }

    @GetMapping("/debit-notes")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> listDebitNotes() {
        return list(DocumentType.DEBIT_NOTE);
    }

    @PostMapping("/debit-notes")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<DocumentDto>> createDebitNote(@Valid @RequestBody DocumentRequest request) {
        return created(purchaseService.createDebitNote(request), "Debit note created");
    }

    @GetMapping("/payments")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<PaymentDto>>> listPayments() {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.listPayments()));
    }

    @PostMapping("/payments")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(purchaseService.recordPayment(request), "Payment recorded"));
    }

    @GetMapping("/payables-ageing")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<AgeingPartyDto>>> payablesAgeing() {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.payablesAgeing()));
    }

    @GetMapping("/reorder-suggestions")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<ReorderSuggestionDto>>> reorderSuggestions() {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.reorderSuggestions()));
    }

    private ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> list(DocumentType type) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.listDocuments(type)));
    }

    private static ResponseEntity<ApiResponse<DocumentDto>> created(DocumentDto document, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(document, message + " — " + document.getDocNumber()));
    }
}
