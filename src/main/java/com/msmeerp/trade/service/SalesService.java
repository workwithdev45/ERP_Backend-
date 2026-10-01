package com.msmeerp.trade.service;

import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.trade.dto.AgeingPartyDto;
import com.msmeerp.trade.dto.ConvertQuotationRequest;
import com.msmeerp.trade.dto.DocumentDto;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.dto.DocumentSummaryDto;
import com.msmeerp.trade.dto.ListQuery;
import com.msmeerp.trade.dto.PaymentDto;
import com.msmeerp.trade.dto.PaymentRequest;
import com.msmeerp.trade.entity.DocumentType;

import java.util.List;

/** W11–W12: quote-to-cash — quotation → sales order (reserves stock) → delivery challan (stock out) → GST invoice → receipt. */
public interface SalesService {
    PagedResponse<DocumentSummaryDto> listDocuments(DocumentType type, ListQuery query);

    DocumentDto getDocument(Long id);

    DocumentDto createQuotation(DocumentRequest request);

    DocumentDto cancelQuotation(Long id);

    /** Turns an open quotation into a confirmed sales order. */
    DocumentDto convertQuotation(Long id, ConvertQuotationRequest request);

    DocumentDto createSalesOrder(DocumentRequest request);

    /** Cancels an order nothing has been delivered against, releasing its reserved stock. */
    DocumentDto cancelSalesOrder(Long id);

    DocumentDto createDeliveryChallan(DocumentRequest request);

    /** Invoices a challan's uninvoiced quantity, or — with no source — a direct invoice that also ships the stock. */
    DocumentDto createInvoice(DocumentRequest request);

    /** Sales return: stock back in, and the note is applied against the invoice. */
    DocumentDto createCreditNote(DocumentRequest request);

    PaymentDto recordReceipt(PaymentRequest request);

    PagedResponse<PaymentDto> listReceipts(ListQuery query);

    List<AgeingPartyDto> receivablesAgeing();
}
