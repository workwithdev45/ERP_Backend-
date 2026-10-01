package com.msmeerp.trade.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.inventory.entity.InventoryItem;
import com.msmeerp.inventory.entity.ItemType;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.repository.InventoryItemRepository;
import com.msmeerp.inventory.repository.ProductRepository;
import com.msmeerp.inventory.service.InventoryService;
import com.msmeerp.trade.dto.AgeingPartyDto;
import com.msmeerp.trade.dto.DocumentDto;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.dto.DocumentSummaryDto;
import com.msmeerp.trade.dto.ListQuery;
import com.msmeerp.trade.dto.PaymentDto;
import com.msmeerp.trade.dto.PaymentRequest;
import com.msmeerp.trade.dto.ReorderSuggestionDto;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.PaymentDirection;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import com.msmeerp.trade.repository.TradeDocumentLineRepository;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private static final Set<DocumentStatus> OPEN_ORDER =
            Set.of(DocumentStatus.DRAFT, DocumentStatus.APPROVED, DocumentStatus.PARTIALLY_RECEIVED);

    private final DocumentEngine engine;
    private final PaymentEngine paymentEngine;
    private final InventoryService inventoryService;
    private final TradeDocumentRepository documentRepository;
    private final TradeDocumentLineRepository lineRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentSummaryDto> listDocuments(DocumentType type, ListQuery query) {
        if (!type.isPurchase()) {
            throw new BadRequestException("Not a purchase document type");
        }
        return engine.page(type, query);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto getDocument(Long id) {
        TradeDocument document = engine.requireDocument(id, null);
        if (!document.getDocType().isPurchase()) {
            throw new BadRequestException(document.getDocNumber() + " is not a purchase document");
        }
        return engine.toDto(document);
    }

    // -- purchase order ------------------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createPurchaseOrder(DocumentRequest request) {
        Party vendor = engine.requireVendor(request.getPartyId());
        engine.requireWarehouse(request.getWarehouseId());
        request.setSourceDocumentId(null);
        request.setReverseCharge(false);

        TradeDocument order = engine.newDocument(DocumentType.PURCHASE_ORDER, DocumentStatus.DRAFT, vendor, request);
        request.getLines().forEach(line -> order.addLine(engine.lineFromProduct(line)));
        engine.recalculate(order);
        return engine.toDto(documentRepository.save(order));
    }

    @Override
    @Transactional
    public DocumentDto approvePurchaseOrder(Long id) {
        TradeDocument order = engine.requireDocument(id, DocumentType.PURCHASE_ORDER);
        engine.requireStatus(order, DocumentStatus.DRAFT);
        order.setStatus(DocumentStatus.APPROVED);
        engine.markApproved(order);
        return engine.toDto(documentRepository.save(order));
    }

    @Override
    @Transactional
    public DocumentDto cancelPurchaseOrder(Long id) {
        TradeDocument order = engine.requireDocument(id, DocumentType.PURCHASE_ORDER);
        engine.requireStatus(order, DocumentStatus.DRAFT, DocumentStatus.APPROVED);
        order.setStatus(DocumentStatus.CANCELLED);
        return engine.toDto(documentRepository.save(order));
    }

    // -- goods receipt -------------------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createGoodsReceipt(DocumentRequest request) {
        TradeDocument order = requireSource(request, DocumentType.PURCHASE_ORDER, "a purchase order");
        engine.requireStatus(order, DocumentStatus.APPROVED, DocumentStatus.PARTIALLY_RECEIVED);
        Long warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : order.getWarehouseId();
        engine.requireWarehouse(warehouseId);
        Map<TradeDocumentLine, Integer> picked = engine.pickSourceLines(order, request.getLines(), TradeDocumentLine::getPendingQuantity);

        request.setWarehouseId(warehouseId);
        request.setReverseCharge(false);
        TradeDocument receipt = engine.newDocument(DocumentType.GOODS_RECEIPT, DocumentStatus.POSTED,
                engine.getParty(order.getPartyId()), request);
        copyTaxContext(order, receipt);
        picked.forEach((orderLine, quantity) -> {
            receipt.addLine(engine.lineFromSource(orderLine, quantity));
            orderLine.setFulfilledQuantity(orderLine.getFulfilledQuantity() + quantity);
        });
        engine.recalculate(receipt);
        documentRepository.save(receipt);

        for (TradeDocumentLine line : receipt.getLines()) {
            if (line.isStockItem()) {
                inventoryService.receiveForDocument(line.getProductId(), warehouseId, line.getQuantity(), engine.unitCost(line),
                        StockMovement.MovementType.IN, "GRN", receipt.getDocNumber(),
                        receipt.getPartyName() + " · " + order.getDocNumber());
            }
        }

        boolean fullyReceived = order.getLines().stream().allMatch(l -> l.getPendingQuantity() == 0);
        order.setStatus(fullyReceived ? DocumentStatus.RECEIVED : DocumentStatus.PARTIALLY_RECEIVED);
        documentRepository.save(order);
        return engine.toDto(receipt);
    }

    // -- bill ----------------------------------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createBill(DocumentRequest request) {
        TradeDocument bill;
        TradeDocument receipt = null;
        if (request.getSourceDocumentId() != null) {
            receipt = requireSource(request, DocumentType.GOODS_RECEIPT, "a goods receipt");
            engine.requireStatus(receipt, DocumentStatus.POSTED);
            Map<TradeDocumentLine, Integer> picked = engine.pickSourceLines(receipt, request.getLines(), TradeDocumentLine::getPendingQuantity);
            Party vendor = engine.getParty(receipt.getPartyId());
            request.setWarehouseId(receipt.getWarehouseId());
            bill = engine.newDocument(DocumentType.PURCHASE_BILL, DocumentStatus.UNPAID, vendor, request);
            copyTaxContext(receipt, bill);
            picked.forEach((receiptLine, quantity) -> {
                bill.addLine(engine.lineFromSource(receiptLine, quantity));
                receiptLine.setFulfilledQuantity(receiptLine.getFulfilledQuantity() + quantity);
            });
            setDueDate(bill, vendor);
        } else {
            Party vendor = engine.requireVendor(request.getPartyId());
            bill = engine.newDocument(DocumentType.PURCHASE_BILL, DocumentStatus.UNPAID, vendor, request);
            request.getLines().forEach(line -> bill.addLine(engine.lineFromProduct(line)));
            if (bill.getLines().stream().anyMatch(TradeDocumentLine::isStockItem)) {
                engine.requireWarehouse(request.getWarehouseId());
            }
            setDueDate(bill, vendor);
        }
        engine.recalculate(bill);
        documentRepository.save(bill);

        if (receipt != null) {
            if (receipt.getLines().stream().allMatch(l -> l.getPendingQuantity() == 0)) {
                receipt.setStatus(DocumentStatus.BILLED);
            }
            documentRepository.save(receipt);
        } else {
            // A direct bill (no GRN) is also the receipt, so stock items come in now.
            for (TradeDocumentLine line : bill.getLines()) {
                if (line.isStockItem()) {
                    inventoryService.receiveForDocument(line.getProductId(), bill.getWarehouseId(), line.getQuantity(),
                            engine.unitCost(line), StockMovement.MovementType.IN, "BILL", bill.getDocNumber(), bill.getPartyName());
                }
            }
        }
        return engine.toDto(bill);
    }

    // -- debit note (purchase return) ----------------------------------------------------------

    @Override
    @Transactional
    public DocumentDto createDebitNote(DocumentRequest request) {
        TradeDocument bill = requireSource(request, DocumentType.PURCHASE_BILL, "a purchase bill");
        engine.requireStatus(bill, DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID, DocumentStatus.PAID);
        // On a bill, fulfilled quantity counts what has already been returned.
        Map<TradeDocumentLine, Integer> picked = engine.pickSourceLines(bill, request.getLines(), TradeDocumentLine::getPendingQuantity);
        Long warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : bill.getWarehouseId();
        boolean hasStock = picked.keySet().stream().anyMatch(TradeDocumentLine::isStockItem);
        if (hasStock) {
            engine.requireWarehouse(warehouseId);
        }

        request.setWarehouseId(warehouseId);
        request.setReverseCharge(bill.getReverseCharge());
        TradeDocument note = engine.newDocument(DocumentType.DEBIT_NOTE, DocumentStatus.POSTED, engine.getParty(bill.getPartyId()), request);
        copyTaxContext(bill, note);
        picked.forEach((billLine, quantity) -> {
            note.addLine(engine.lineFromSource(billLine, quantity));
            billLine.setFulfilledQuantity(billLine.getFulfilledQuantity() + quantity);
        });
        engine.recalculate(note);
        documentRepository.save(note);

        for (TradeDocumentLine line : note.getLines()) {
            if (line.isStockItem()) {
                inventoryService.issueForDocument(line.getProductId(), warehouseId, line.getQuantity(),
                        StockMovement.MovementType.RETURN, "DEBIT_NOTE", note.getDocNumber(),
                        "Returned to " + note.getPartyName() + " · " + bill.getDocNumber());
            }
        }

        // The return reduces what we owe on the bill; anything beyond its balance is a credit with the vendor.
        BigDecimal applied = note.getTotalAmount().min(bill.getBalance());
        if (applied.signum() > 0) {
            engine.settle(bill, applied);
        }
        // On a note, settled = the part applied to the bill; its balance is credit still owed to the party.
        note.setSettledAmount(applied);
        documentRepository.save(note);
        documentRepository.save(bill);
        return engine.toDto(note);
    }

    // -- payments & reports --------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentDto recordPayment(PaymentRequest request) {
        Party vendor = engine.requireVendor(request.getPartyId());
        return paymentEngine.record(PaymentDirection.PAID, vendor, DocumentType.PURCHASE_BILL, request);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentDto> listPayments(ListQuery query) {
        return paymentEngine.list(PaymentDirection.PAID, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgeingPartyDto> payablesAgeing() {
        return paymentEngine.ageing(DocumentType.PURCHASE_BILL);
    }

    /**
     * Items whose available-to-promise stock plus open purchase orders is at or below the reorder
     * level. Suggests topping up to twice the reorder level, from the last vendor at the last rate.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReorderSuggestionDto> reorderSuggestions() {
        String tenantId = engine.tenantId();
        Map<Long, Integer> available = new HashMap<>();
        for (InventoryItem item : inventoryItemRepository.findAllWithDetails(tenantId)) {
            available.merge(item.getProduct().getId(), item.getAvailableQuantity() - item.getReservedQuantity(), Integer::sum);
        }

        Map<Long, Integer> onOrder = new HashMap<>();
        for (TradeDocumentLine line : lineRepository.findByDocumentTypeAndStatus(tenantId, DocumentType.PURCHASE_ORDER, OPEN_ORDER)) {
            onOrder.merge(line.getProductId(), line.getPendingQuantity(), Integer::sum);
        }

        Map<Long, TradeDocumentLine> lastOrdered = new HashMap<>();
        Set<DocumentStatus> anyOrder = Set.of(DocumentStatus.DRAFT, DocumentStatus.APPROVED, DocumentStatus.PARTIALLY_RECEIVED, DocumentStatus.RECEIVED);
        for (TradeDocumentLine line : lineRepository.findByDocumentTypeAndStatus(tenantId, DocumentType.PURCHASE_ORDER, anyOrder)) {
            lastOrdered.putIfAbsent(line.getProductId(), line);
        }

        List<ReorderSuggestionDto> suggestions = new ArrayList<>();
        for (Product product : productRepository.findByTenantIdOrderByNameAsc(tenantId)) {
            if (product.getItemType() != ItemType.STOCK || !Boolean.TRUE.equals(product.getActive()) || product.getReorderLevel() <= 0) {
                continue;
            }
            int have = available.getOrDefault(product.getId(), 0);
            int ordered = onOrder.getOrDefault(product.getId(), 0);
            if (have + ordered > product.getReorderLevel()) {
                continue;
            }
            TradeDocumentLine last = lastOrdered.get(product.getId());
            suggestions.add(ReorderSuggestionDto.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    .uom(product.getUnitOfMeasure())
                    .reorderLevel(product.getReorderLevel())
                    .available(have)
                    .onOrder(ordered)
                    .suggestedQuantity(Math.max(product.getReorderLevel() * 2 - have - ordered, 1))
                    .lastRate(last != null ? last.getRate() : null)
                    .gstRate(product.getGstRatePercent())
                    .lastVendorId(last != null ? last.getDocument().getPartyId() : null)
                    .lastVendorName(last != null ? last.getDocument().getPartyName() : null)
                    .build());
        }
        return suggestions;
    }

    // -- helpers -------------------------------------------------------------------------------

    private TradeDocument requireSource(DocumentRequest request, DocumentType type, String what) {
        if (request.getSourceDocumentId() == null) {
            throw new BadRequestException("Choose " + what);
        }
        return engine.requireDocument(request.getSourceDocumentId(), type);
    }

    /** Follow-on documents keep the tax treatment of the document they came from. */
    private static void copyTaxContext(TradeDocument from, TradeDocument to) {
        to.setPlaceOfSupply(from.getPlaceOfSupply());
        to.setInterState(from.getInterState());
    }

    private static void setDueDate(TradeDocument bill, Party vendor) {
        if (bill.getDueDate() == null) {
            bill.setDueDate(bill.getDocDate().plusDays(vendor.getPaymentTermsDays()));
        }
    }
}
