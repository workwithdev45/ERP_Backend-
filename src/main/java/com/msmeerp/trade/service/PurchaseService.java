package com.msmeerp.trade.service;

import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.trade.dto.AgeingPartyDto;
import com.msmeerp.trade.dto.DocumentDto;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.dto.DocumentSummaryDto;
import com.msmeerp.trade.dto.ListQuery;
import com.msmeerp.trade.dto.PaymentDto;
import com.msmeerp.trade.dto.PaymentRequest;
import com.msmeerp.trade.dto.ReorderSuggestionDto;
import com.msmeerp.trade.entity.DocumentType;

import java.util.List;

/** W9–W10: procure-to-pay — PO (with approval) → GRN (stock in) → bill → payment, plus returns. */
public interface PurchaseService {
    PagedResponse<DocumentSummaryDto> listDocuments(DocumentType type, ListQuery query);

    DocumentDto getDocument(Long id);

    DocumentDto createPurchaseOrder(DocumentRequest request);

    DocumentDto approvePurchaseOrder(Long id);

    DocumentDto cancelPurchaseOrder(Long id);

    /** Receives some or all of an approved PO's pending quantity into a warehouse. */
    DocumentDto createGoodsReceipt(DocumentRequest request);

    /** Bills a GRN's unbilled quantity, or — with no source — records a direct bill (receiving any stock items). */
    DocumentDto createBill(DocumentRequest request);

    /** Returns billed goods to the vendor: stock out, and the note is applied against the bill. */
    DocumentDto createDebitNote(DocumentRequest request);

    PaymentDto recordPayment(PaymentRequest request);

    PagedResponse<PaymentDto> listPayments(ListQuery query);

    List<AgeingPartyDto> payablesAgeing();

    List<ReorderSuggestionDto> reorderSuggestions();
}
