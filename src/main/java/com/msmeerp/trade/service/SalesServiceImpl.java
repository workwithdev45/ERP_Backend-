package com.msmeerp.trade.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.service.InventoryService;
import com.msmeerp.trade.dto.AgeingPartyDto;
import com.msmeerp.trade.dto.ConvertQuotationRequest;
import com.msmeerp.trade.dto.DocumentDto;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.dto.DocumentSummaryDto;
import com.msmeerp.trade.dto.ListQuery;
import com.msmeerp.trade.dto.PaymentDto;
import com.msmeerp.trade.dto.PaymentRequest;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.PaymentDirection;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private static final int DEFAULT_QUOTATION_VALIDITY_DAYS = 15;

    private final DocumentEngine engine;
    private final PaymentEngine paymentEngine;
    private final InventoryService inventoryService;
    private final TradeDocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentSummaryDto> listDocuments(DocumentType type, ListQuery query) {
        if (type.isPurchase()) {
            throw new BadRequestException("Not a sales document type");
        }
        return engine.page(type, query);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto getDocument(Long id) {
        TradeDocument document = engine.requireDocument(id, null);
        if (document.getDocType().isPurchase()) {
            throw new BadRequestException(document.getDocNumber() + " is not a sales document");
        }
        return engine.toDto(document);
    }

    // -- quotation -----------------------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createQuotation(DocumentRequest request) {
        Party customer = engine.requireCustomer(request.getPartyId());
        request.setSourceDocumentId(null);
        request.setReverseCharge(false);
        TradeDocument quotation = engine.newDocument(DocumentType.QUOTATION, DocumentStatus.OPEN, customer, request);
        if (quotation.getDueDate() == null) {
            quotation.setDueDate(quotation.getDocDate().plusDays(DEFAULT_QUOTATION_VALIDITY_DAYS));
        }
        request.getLines().forEach(line -> quotation.addLine(engine.lineFromProduct(line)));
        engine.recalculate(quotation);
        return engine.toDto(documentRepository.save(quotation));
    }

    @Override
    @Transactional
    public DocumentDto cancelQuotation(Long id) {
        TradeDocument quotation = engine.requireDocument(id, DocumentType.QUOTATION);
        engine.requireStatus(quotation, DocumentStatus.OPEN);
        quotation.setStatus(DocumentStatus.CANCELLED);
        return engine.toDto(documentRepository.save(quotation));
    }

    @Override
    @Transactional
    public DocumentDto convertQuotation(Long id, ConvertQuotationRequest request) {
        TradeDocument quotation = engine.requireDocument(id, DocumentType.QUOTATION);
        engine.requireStatus(quotation, DocumentStatus.OPEN);
        Party customer = engine.requireCustomer(quotation.getPartyId());
        engine.requireWarehouse(request.getWarehouseId());

        DocumentRequest orderRequest = DocumentRequest.builder()
                .warehouseId(request.getWarehouseId())
                .dueDate(request.getDueDate())
                .partyReference(request.getPartyReference())
                .sourceDocumentId(quotation.getId())
                .notes(quotation.getNotes())
                .build();
        TradeDocument order = engine.newDocument(DocumentType.SALES_ORDER, DocumentStatus.CONFIRMED, customer, orderRequest);
        quotation.getLines().forEach(line -> order.addLine(engine.lineFromSource(line, line.getQuantity())));
        confirmOrder(order, customer);

        quotation.setStatus(DocumentStatus.CONVERTED);
        documentRepository.save(quotation);
        return engine.toDto(order);
    }

    // -- sales order ---------------------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createSalesOrder(DocumentRequest request) {
        Party customer = engine.requireCustomer(request.getPartyId());
        engine.requireWarehouse(request.getWarehouseId());
        request.setSourceDocumentId(null);
        request.setReverseCharge(false);
        TradeDocument order = engine.newDocument(DocumentType.SALES_ORDER, DocumentStatus.CONFIRMED, customer, request);
        request.getLines().forEach(line -> order.addLine(engine.lineFromProduct(line)));
        confirmOrder(order, customer);
        return engine.toDto(order);
    }

    /** Credit check, save, then reserve what stock is available for each line. */
    private void confirmOrder(TradeDocument order, Party customer) {
        engine.recalculate(order);
        checkCreditLimit(customer, order.getTotalAmount());
        documentRepository.save(order);
        for (TradeDocumentLine line : order.getLines()) {
            if (line.isStockItem()) {
                line.setReservedQuantity(inventoryService.reserveAvailable(line.getProductId(), order.getWarehouseId(), line.getQuantity()));
            }
        }
        documentRepository.save(order);
    }

    @Override
    @Transactional
    public DocumentDto cancelSalesOrder(Long id) {
        TradeDocument order = engine.requireDocument(id, DocumentType.SALES_ORDER);
        engine.requireStatus(order, DocumentStatus.CONFIRMED);
        if (order.getLines().stream().anyMatch(l -> l.getFulfilledQuantity() > 0)) {
            throw new BadRequestException(order.getDocNumber() + " has deliveries against it and can't be cancelled");
        }
        for (TradeDocumentLine line : order.getLines()) {
            inventoryService.releaseReserved(line.getProductId(), order.getWarehouseId(), line.getReservedQuantity());
            line.setReservedQuantity(0);
        }
        order.setStatus(DocumentStatus.CANCELLED);
        return engine.toDto(documentRepository.save(order));
    }

    // -- delivery challan ----------------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createDeliveryChallan(DocumentRequest request) {
        TradeDocument order = requireSource(request, DocumentType.SALES_ORDER, "a sales order");
        engine.requireStatus(order, DocumentStatus.CONFIRMED, DocumentStatus.PARTIALLY_DELIVERED);
        Map<TradeDocumentLine, Integer> picked = engine.pickSourceLines(order, request.getLines(), TradeDocumentLine::getPendingQuantity);

        request.setWarehouseId(order.getWarehouseId());
        request.setReverseCharge(false);
        TradeDocument challan = engine.newDocument(DocumentType.DELIVERY_CHALLAN, DocumentStatus.POSTED,
                engine.getParty(order.getPartyId()), request);
        copyTaxContext(order, challan);
        picked.forEach((orderLine, quantity) -> challan.addLine(engine.lineFromSource(orderLine, quantity)));
        engine.recalculate(challan);
        documentRepository.save(challan);

        picked.forEach((orderLine, quantity) -> {
            if (orderLine.isStockItem()) {
                // Ship from this order's own reservation first, then from free stock.
                int fromReservation = Math.min(quantity, orderLine.getReservedQuantity());
                inventoryService.releaseReserved(orderLine.getProductId(), order.getWarehouseId(), fromReservation);
                orderLine.setReservedQuantity(orderLine.getReservedQuantity() - fromReservation);
                inventoryService.issueForDocument(orderLine.getProductId(), order.getWarehouseId(), quantity,
                        StockMovement.MovementType.OUT, "DELIVERY", challan.getDocNumber(),
                        challan.getPartyName() + " · " + order.getDocNumber());
            }
            orderLine.setFulfilledQuantity(orderLine.getFulfilledQuantity() + quantity);
        });

        boolean fullyDelivered = order.getLines().stream().allMatch(l -> l.getPendingQuantity() == 0);
        order.setStatus(fullyDelivered ? DocumentStatus.DELIVERED : DocumentStatus.PARTIALLY_DELIVERED);
        documentRepository.save(order);
        return engine.toDto(challan);
    }

    // -- invoice -------------------------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createInvoice(DocumentRequest request) {
        request.setReverseCharge(false);
        TradeDocument invoice;
        TradeDocument challan = null;
        Party customer;
        if (request.getSourceDocumentId() != null) {
            challan = requireSource(request, DocumentType.DELIVERY_CHALLAN, "a delivery challan");
            engine.requireStatus(challan, DocumentStatus.POSTED);
            Map<TradeDocumentLine, Integer> picked = engine.pickSourceLines(challan, request.getLines(), TradeDocumentLine::getPendingQuantity);
            customer = engine.getParty(challan.getPartyId());
            request.setWarehouseId(challan.getWarehouseId());
            invoice = engine.newDocument(DocumentType.SALES_INVOICE, DocumentStatus.UNPAID, customer, request);
            copyTaxContext(challan, invoice);
            picked.forEach((challanLine, quantity) -> {
                invoice.addLine(engine.lineFromSource(challanLine, quantity));
                challanLine.setFulfilledQuantity(challanLine.getFulfilledQuantity() + quantity);
            });
        } else {
            customer = engine.requireCustomer(request.getPartyId());
            invoice = engine.newDocument(DocumentType.SALES_INVOICE, DocumentStatus.UNPAID, customer, request);
            request.getLines().forEach(line -> invoice.addLine(engine.lineFromProduct(line)));
            if (invoice.getLines().stream().anyMatch(TradeDocumentLine::isStockItem)) {
                engine.requireWarehouse(request.getWarehouseId());
            }
        }
        if (invoice.getDueDate() == null) {
            invoice.setDueDate(invoice.getDocDate().plusDays(customer.getPaymentTermsDays()));
        }
        engine.recalculate(invoice);
        if (challan == null) {
            // Invoicing a challan only bills exposure the order already counted; a direct invoice is new exposure.
            checkCreditLimit(customer, invoice.getTotalAmount());
        }
        documentRepository.save(invoice);

        if (challan != null) {
            if (challan.getLines().stream().allMatch(l -> l.getPendingQuantity() == 0)) {
                challan.setStatus(DocumentStatus.INVOICED);
            }
            documentRepository.save(challan);
        } else {
            // A direct invoice is also the delivery, so stock items leave now.
            for (TradeDocumentLine line : invoice.getLines()) {
                if (line.isStockItem()) {
                    inventoryService.issueForDocument(line.getProductId(), invoice.getWarehouseId(), line.getQuantity(),
                            StockMovement.MovementType.OUT, "INVOICE", invoice.getDocNumber(), invoice.getPartyName());
                }
            }
        }
        return engine.toDto(invoice);
    }

    // -- credit note (sales return) ------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createCreditNote(DocumentRequest request) {
        TradeDocument invoice = requireSource(request, DocumentType.SALES_INVOICE, "a sales invoice");
        engine.requireStatus(invoice, DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID, DocumentStatus.PAID);
        // On an invoice, fulfilled quantity counts what has already been returned.
        Map<TradeDocumentLine, Integer> picked = engine.pickSourceLines(invoice, request.getLines(), TradeDocumentLine::getPendingQuantity);
        Long warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : invoice.getWarehouseId();
        if (picked.keySet().stream().anyMatch(TradeDocumentLine::isStockItem)) {
            engine.requireWarehouse(warehouseId);
        }

        request.setWarehouseId(warehouseId);
        request.setReverseCharge(false);
        TradeDocument note = engine.newDocument(DocumentType.CREDIT_NOTE, DocumentStatus.POSTED, engine.getParty(invoice.getPartyId()), request);
        copyTaxContext(invoice, note);
        picked.forEach((invoiceLine, quantity) -> {
            note.addLine(engine.lineFromSource(invoiceLine, quantity));
            invoiceLine.setFulfilledQuantity(invoiceLine.getFulfilledQuantity() + quantity);
        });
        engine.recalculate(note);
        documentRepository.save(note);

        for (TradeDocumentLine line : note.getLines()) {
            if (line.isStockItem()) {
                // No cost given: returned goods come back at the current average cost.
                inventoryService.receiveForDocument(line.getProductId(), warehouseId, line.getQuantity(), null,
                        StockMovement.MovementType.RETURN, "CREDIT_NOTE", note.getDocNumber(),
                        "Returned by " + note.getPartyName() + " · " + invoice.getDocNumber());
            }
        }

        BigDecimal applied = note.getTotalAmount().min(invoice.getBalance());
        if (applied.signum() > 0) {
            engine.settle(invoice, applied);
        }
        // On a note, settled = the part applied to the invoice; its balance is credit still owed to the party.
        note.setSettledAmount(applied);
        documentRepository.save(note);
        documentRepository.save(invoice);
        return engine.toDto(note);
    }

    // -- receipts & reports --------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentDto recordReceipt(PaymentRequest request) {
        Party customer = engine.requireCustomer(request.getPartyId());
        return paymentEngine.record(PaymentDirection.RECEIVED, customer, DocumentType.SALES_INVOICE, request);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentDto> listReceipts(ListQuery query) {
        return paymentEngine.list(PaymentDirection.RECEIVED, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgeingPartyDto> receivablesAgeing() {
        return paymentEngine.ageing(DocumentType.SALES_INVOICE);
    }

    // -- helpers -------------------------------------------------------------------------------

    /**
     * Refuses new business once the customer's exposure would pass their credit limit. Exposure is
     * unpaid invoices + undelivered orders + delivered-but-uninvoiced challans.
     */
    private void checkCreditLimit(Party customer, BigDecimal additional) {
        if (customer.getCreditLimit() == null) {
            return;
        }
        String tenantId = engine.tenantId();
        BigDecimal unpaid = documentRepository.findByTenantIdAndDocTypeAndPartyIdAndStatusIn(tenantId, DocumentType.SALES_INVOICE,
                        customer.getId(), Set.of(DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID)).stream()
                .map(TradeDocument::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal undelivered = pendingValue(documentRepository.findByTenantIdAndDocTypeAndPartyIdAndStatusIn(tenantId,
                DocumentType.SALES_ORDER, customer.getId(), Set.of(DocumentStatus.CONFIRMED, DocumentStatus.PARTIALLY_DELIVERED)));
        BigDecimal uninvoiced = pendingValue(documentRepository.findByTenantIdAndDocTypeAndPartyIdAndStatusIn(tenantId,
                DocumentType.DELIVERY_CHALLAN, customer.getId(), Set.of(DocumentStatus.POSTED)));
        BigDecimal exposure = unpaid.add(undelivered).add(uninvoiced);
        if (exposure.add(additional).compareTo(customer.getCreditLimit()) > 0) {
            throw new BadRequestException("Credit limit exceeded for " + customer.getName() + ": limit ₹"
                    + customer.getCreditLimit().setScale(2, RoundingMode.HALF_UP) + ", already outstanding ₹"
                    + exposure.setScale(2, RoundingMode.HALF_UP) + ", this document ₹" + additional.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static BigDecimal pendingValue(List<TradeDocument> documents) {
        BigDecimal value = BigDecimal.ZERO;
        for (TradeDocument document : documents) {
            for (TradeDocumentLine line : document.getLines()) {
                if (line.getPendingQuantity() > 0) {
                    value = value.add(line.getLineTotal().multiply(BigDecimal.valueOf(line.getPendingQuantity()))
                            .divide(BigDecimal.valueOf(line.getQuantity()), 2, RoundingMode.HALF_UP));
                }
            }
        }
        return value;
    }

    private TradeDocument requireSource(DocumentRequest request, DocumentType type, String what) {
        if (request.getSourceDocumentId() == null) {
            throw new BadRequestException("Choose " + what);
        }
        return engine.requireDocument(request.getSourceDocumentId(), type);
    }

    private static void copyTaxContext(TradeDocument from, TradeDocument to) {
        to.setPlaceOfSupply(from.getPlaceOfSupply());
        to.setInterState(from.getInterState());
    }
}
