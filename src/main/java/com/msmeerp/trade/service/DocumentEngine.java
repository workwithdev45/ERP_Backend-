package com.msmeerp.trade.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.util.SecurityUtils;
import com.msmeerp.inventory.entity.Product;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.ProductRepository;
import com.msmeerp.inventory.repository.WarehouseRepository;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.trade.dto.AllocationDto;
import com.msmeerp.trade.dto.DocumentDto;
import com.msmeerp.trade.dto.DocumentLineDto;
import com.msmeerp.trade.dto.DocumentLineRequest;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.dto.DocumentSummaryDto;
import com.msmeerp.trade.dto.ListQuery;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.PaymentDirection;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import com.msmeerp.trade.repository.PartyRepository;
import com.msmeerp.trade.repository.PaymentAllocationRepository;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import com.msmeerp.trade.repository.TradeSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * W6 document engine: what every purchase and sales document shares — numbering, party snapshot,
 * GST place-of-supply split, line and total calculation, settlement and mapping to DTOs.
 */
@Component
@RequiredArgsConstructor
public class DocumentEngine {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final JdbcTemplate jdbcTemplate;
    private final TenantRepository tenantRepository;
    private final PartyRepository partyRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final TradeDocumentRepository documentRepository;
    private final PaymentAllocationRepository allocationRepository;

    public String tenantId() {
        return TenantContext.getTenantId();
    }

    public String currentUser() {
        return SecurityUtils.getCurrentUsername().orElse("system");
    }

    // -- lookups -------------------------------------------------------------------------------

    public Party requireCustomer(Long partyId) {
        Party party = requireParty(partyId);
        if (!party.getPartyType().isCustomer()) {
            throw new BadRequestException(party.getName() + " is not set up as a customer");
        }
        return party;
    }

    public Party requireVendor(Long partyId) {
        Party party = requireParty(partyId);
        if (!party.getPartyType().isVendor()) {
            throw new BadRequestException(party.getName() + " is not set up as a vendor");
        }
        return party;
    }

    private Party requireParty(Long partyId) {
        if (partyId == null) {
            throw new BadRequestException("Choose a party");
        }
        Party party = partyRepository.findByTenantIdAndId(tenantId(), partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found with id: " + partyId));
        if (!Boolean.TRUE.equals(party.getActive())) {
            throw new BadRequestException(party.getName() + " is inactive");
        }
        return party;
    }

    public Party getParty(Long partyId) {
        return partyRepository.findByTenantIdAndId(tenantId(), partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found with id: " + partyId));
    }

    public TradeDocument requireDocument(Long id, DocumentType type) {
        TradeDocument document = documentRepository.findByTenantIdAndId(tenantId(), id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (type != null && document.getDocType() != type) {
            throw new BadRequestException(document.getDocNumber() + " is not a " + label(type));
        }
        return document;
    }

    public Warehouse requireWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new BadRequestException("Choose a warehouse");
        }
        return warehouseRepository.findByTenantIdAndId(tenantId(), warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
    }

    public void requireStatus(TradeDocument document, DocumentStatus... allowed) {
        for (DocumentStatus status : allowed) {
            if (document.getStatus() == status) {
                return;
            }
        }
        throw new BadRequestException(document.getDocNumber() + " is " + humanStatus(document.getStatus())
                + " and can't be used for this");
    }

    // -- creation ------------------------------------------------------------------------------

    /** A new document header for {@code party}, numbered and with GST place of supply resolved. */
    public TradeDocument newDocument(DocumentType type, DocumentStatus status, Party party, DocumentRequest request) {
        LocalDate docDate = request.getDocDate() != null ? request.getDocDate() : LocalDate.now();
        TradeDocument document = TradeDocument.builder()
                .docType(type)
                .docNumber(nextNumber(type))
                .status(status)
                .partyId(party.getId())
                .partyName(party.getName())
                .partyGstin(party.getGstin())
                .docDate(docDate)
                .dueDate(request.getDueDate())
                .warehouseId(request.getWarehouseId())
                .sourceDocumentId(request.getSourceDocumentId())
                .partyReference(trimToNull(request.getPartyReference()))
                .reverseCharge(Boolean.TRUE.equals(request.getReverseCharge()))
                .notes(trimToNull(request.getNotes()))
                .createdBy(currentUser())
                .build();
        applyPlaceOfSupply(document, party);
        document.setTenantId(tenantId());
        return document;
    }

    /** A line for a fresh document, snapshotting the product; rate/GST default from the product. */
    public TradeDocumentLine lineFromProduct(DocumentLineRequest request) {
        if (request.getProductId() == null) {
            throw new BadRequestException("Choose an item on every line");
        }
        Product product = productRepository.findByTenantIdAndId(tenantId(), request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + request.getProductId()));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException(product.getName() + " is inactive");
        }
        TradeDocumentLine line = TradeDocumentLine.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .hsnCode(product.getHsnCode())
                .uom(product.getUnitOfMeasure())
                .itemType(product.getItemType())
                .quantity(request.getQuantity())
                .rate(orZero(request.getRate()))
                .discountPercent(orZero(request.getDiscountPercent()))
                .gstRate(request.getGstRate() != null ? request.getGstRate() : orZero(product.getGstRatePercent()))
                .build();
        line.setTenantId(tenantId());
        return line;
    }

    /** A line copied from the source document's line (GRN from PO line, invoice from challan line, ...). */
    public TradeDocumentLine lineFromSource(TradeDocumentLine source, int quantity) {
        TradeDocumentLine line = TradeDocumentLine.builder()
                .productId(source.getProductId())
                .productName(source.getProductName())
                .sku(source.getSku())
                .hsnCode(source.getHsnCode())
                .uom(source.getUom())
                .itemType(source.getItemType())
                .quantity(quantity)
                .rate(source.getRate())
                .discountPercent(source.getDiscountPercent())
                .gstRate(source.getGstRate())
                .sourceLineId(source.getId())
                .build();
        line.setTenantId(tenantId());
        return line;
    }

    /**
     * Resolves each request line against the source document: the line must belong to it and not
     * exceed what's still pending. Returns source line → requested quantity, skipping zero lines.
     */
    public Map<TradeDocumentLine, Integer> pickSourceLines(TradeDocument source, List<DocumentLineRequest> requests,
                                                           Function<TradeDocumentLine, Integer> pendingOf) {
        Map<Long, TradeDocumentLine> byId = source.getLines().stream()
                .collect(Collectors.toMap(TradeDocumentLine::getId, Function.identity()));
        Map<TradeDocumentLine, Integer> picked = new java.util.LinkedHashMap<>();
        for (DocumentLineRequest request : requests) {
            TradeDocumentLine sourceLine = byId.get(request.getSourceLineId());
            if (sourceLine == null) {
                throw new BadRequestException("Each line must come from " + source.getDocNumber());
            }
            int pending = pendingOf.apply(sourceLine);
            if (request.getQuantity() > pending) {
                throw new BadRequestException(sourceLine.getProductName() + ": only " + pending + " left on "
                        + source.getDocNumber());
            }
            picked.merge(sourceLine, request.getQuantity(), Integer::sum);
        }
        if (picked.isEmpty()) {
            throw new BadRequestException("Add at least one line");
        }
        return picked;
    }

    // -- GST -----------------------------------------------------------------------------------

    /**
     * Intra-state supply → CGST + SGST, inter-state → IGST. Place of supply is the party's state;
     * a party with no state (e.g. an unregistered walk-in customer) is treated as in-state.
     */
    private void applyPlaceOfSupply(TradeDocument document, Party party) {
        Tenant company = tenantRepository.findById(tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        if (!StringUtils.hasText(company.getState())) {
            throw new BadRequestException("Add your company's state in Settings → Company first — GST needs it "
                    + "to decide between CGST + SGST and IGST");
        }
        String placeOfSupply = StringUtils.hasText(party.getState()) ? party.getState().trim() : company.getState().trim();
        document.setPlaceOfSupply(placeOfSupply);
        document.setInterState(!placeOfSupply.equalsIgnoreCase(company.getState().trim()));
    }

    /** Recomputes every line's tax and the document totals; billing documents round to the rupee. */
    public void recalculate(TradeDocument document) {
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        for (TradeDocumentLine line : document.getLines()) {
            BigDecimal gross = line.getRate().multiply(BigDecimal.valueOf(line.getQuantity()));
            BigDecimal discount = gross.multiply(line.getDiscountPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal lineTaxable = gross.subtract(discount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = lineTaxable.multiply(line.getGstRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            line.setTaxableAmount(lineTaxable);
            if (Boolean.TRUE.equals(document.getInterState())) {
                line.setIgstAmount(tax);
                line.setCgstAmount(BigDecimal.ZERO);
                line.setSgstAmount(BigDecimal.ZERO);
            } else {
                BigDecimal half = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                line.setCgstAmount(half);
                line.setSgstAmount(tax.subtract(half));
                line.setIgstAmount(BigDecimal.ZERO);
            }
            line.setLineTotal(lineTaxable.add(tax));

            taxable = taxable.add(line.getTaxableAmount());
            cgst = cgst.add(line.getCgstAmount());
            sgst = sgst.add(line.getSgstAmount());
            igst = igst.add(line.getIgstAmount());
        }

        document.setTaxableAmount(taxable);
        document.setCgstAmount(cgst);
        document.setSgstAmount(sgst);
        document.setIgstAmount(igst);

        // Under reverse charge the buyer pays the GST to the government, so the vendor is owed only the taxable value.
        BigDecimal payable = Boolean.TRUE.equals(document.getReverseCharge())
                ? taxable
                : taxable.add(cgst).add(sgst).add(igst);
        if (document.getDocType().isBilling()) {
            BigDecimal rounded = payable.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
            document.setRoundOff(rounded.subtract(payable));
            document.setTotalAmount(rounded);
        } else {
            document.setRoundOff(BigDecimal.ZERO);
            document.setTotalAmount(payable);
        }
    }

    /** Effective cost per unit of a line after discount, used to value stock received. */
    public BigDecimal unitCost(TradeDocumentLine line) {
        return line.getTaxableAmount().divide(BigDecimal.valueOf(line.getQuantity()), 4, RoundingMode.HALF_UP);
    }

    // -- settlement ----------------------------------------------------------------------------

    /** Applies a payment or debit/credit note to a bill/invoice and moves its status along. */
    public void settle(TradeDocument document, BigDecimal amount) {
        document.setSettledAmount(document.getSettledAmount().add(amount));
        refreshPaymentStatus(document);
    }

    public void refreshPaymentStatus(TradeDocument document) {
        if (document.getSettledAmount().compareTo(document.getTotalAmount()) >= 0) {
            document.setStatus(DocumentStatus.PAID);
        } else if (document.getSettledAmount().signum() > 0) {
            document.setStatus(DocumentStatus.PARTIALLY_PAID);
        } else {
            document.setStatus(DocumentStatus.UNPAID);
        }
    }

    // -- numbering -----------------------------------------------------------------------------

    public String nextNumber(DocumentType type) {
        return type.getPrefix() + "-" + String.format("%04d", nextSequence(type.name()));
    }

    public String nextPaymentNumber(PaymentDirection direction) {
        String key = direction == PaymentDirection.RECEIVED ? "RECEIPT" : "PAYMENT";
        String prefix = direction == PaymentDirection.RECEIVED ? "RCPT" : "PAY";
        return prefix + "-" + String.format("%04d", nextSequence(key));
    }

    /** Atomic per-tenant counter: the upsert takes a row lock, so concurrent documents never share a number. */
    private long nextSequence(String key) {
        Long next = jdbcTemplate.queryForObject(
                "INSERT INTO document_sequences (tenant_id, doc_type, next_number) VALUES (?, ?, 2) " +
                        "ON CONFLICT (tenant_id, doc_type) DO UPDATE SET next_number = document_sequences.next_number + 1 " +
                        "RETURNING next_number - 1",
                Long.class, tenantId(), key);
        return Objects.requireNonNull(next);
    }

    // -- mapping -------------------------------------------------------------------------------

    /** One page of a document list, newest first, filtered by {@code query}. */
    public PagedResponse<DocumentSummaryDto> page(DocumentType type, ListQuery query) {
        Page<TradeDocument> page = documentRepository.findAll(
                TradeSpecifications.documents(tenantId(), type, query.getStatus(), query.getPartyId(), query.getQ()), query.pageable());
        List<DocumentSummaryDto> content = toSummaries(page.getContent());
        return PagedResponse.<DocumentSummaryDto>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public List<DocumentSummaryDto> toSummaries(List<TradeDocument> documents) {
        Map<Long, String> warehouseNames = warehouseNames();
        Map<Long, String> sourceNumbers = documentNumbers(documents.stream()
                .map(TradeDocument::getSourceDocumentId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return documents.stream()
                .map(d -> fillSummary(DocumentSummaryDto.builder(), d, warehouseNames, sourceNumbers).build())
                .collect(Collectors.toList());
    }

    public DocumentDto toDto(TradeDocument document) {
        Map<Long, String> warehouseNames = warehouseNames();
        Map<Long, String> sourceNumbers = document.getSourceDocumentId() == null
                ? Map.of()
                : documentNumbers(List.of(document.getSourceDocumentId()));
        List<TradeDocument> linked = documentRepository.findByTenantIdAndSourceDocumentIdOrderByIdAsc(tenantId(), document.getId());
        List<AllocationDto> payments = allocationRepository.findByDocument(tenantId(), document.getId()).stream()
                .map(a -> AllocationDto.builder()
                        .paymentId(a.getPayment().getId())
                        .paymentNumber(a.getPayment().getPaymentNumber())
                        .paymentDate(a.getPayment().getPaymentDate())
                        .mode(a.getPayment().getMode().name())
                        .documentId(document.getId())
                        .documentNumber(document.getDocNumber())
                        .amount(a.getAmount())
                        .build())
                .collect(Collectors.toList());

        return fillSummary(DocumentDto.builder(), document, warehouseNames, sourceNumbers)
                .partyGstin(document.getPartyGstin())
                .placeOfSupply(document.getPlaceOfSupply())
                .notes(document.getNotes())
                .createdBy(document.getCreatedBy())
                .approvedBy(document.getApprovedBy())
                .approvedAt(document.getApprovedAt())
                .lines(document.getLines().stream().map(this::toLineDto).collect(Collectors.toList()))
                .linkedDocuments(toSummaries(linked))
                .payments(payments)
                .build();
    }

    private <B extends DocumentSummaryDto.DocumentSummaryDtoBuilder<?, ?>> B fillSummary(
            B builder, TradeDocument d, Map<Long, String> warehouseNames, Map<Long, String> sourceNumbers) {
        builder.id(d.getId())
                .docType(d.getDocType().name())
                .docNumber(d.getDocNumber())
                .status(d.getStatus().name())
                .partyId(d.getPartyId())
                .partyName(d.getPartyName())
                .docDate(d.getDocDate())
                .dueDate(d.getDueDate())
                .warehouseId(d.getWarehouseId())
                .warehouseName(d.getWarehouseId() == null ? null : warehouseNames.get(d.getWarehouseId()))
                .sourceDocumentId(d.getSourceDocumentId())
                .sourceDocumentNumber(d.getSourceDocumentId() == null ? null : sourceNumbers.get(d.getSourceDocumentId()))
                .partyReference(d.getPartyReference())
                .interState(d.getInterState())
                .reverseCharge(d.getReverseCharge())
                .taxableAmount(d.getTaxableAmount())
                .cgstAmount(d.getCgstAmount())
                .sgstAmount(d.getSgstAmount())
                .igstAmount(d.getIgstAmount())
                .roundOff(d.getRoundOff())
                .totalAmount(d.getTotalAmount())
                .settledAmount(d.getSettledAmount())
                .balance(d.getBalance());
        return builder;
    }

    private DocumentLineDto toLineDto(TradeDocumentLine line) {
        return DocumentLineDto.builder()
                .id(line.getId())
                .lineNo(line.getLineNo())
                .productId(line.getProductId())
                .productName(line.getProductName())
                .sku(line.getSku())
                .hsnCode(line.getHsnCode())
                .uom(line.getUom())
                .itemType(line.getItemType().name())
                .quantity(line.getQuantity())
                .fulfilledQuantity(line.getFulfilledQuantity())
                .pendingQuantity(line.getPendingQuantity())
                .reservedQuantity(line.getReservedQuantity())
                .rate(line.getRate())
                .discountPercent(line.getDiscountPercent())
                .gstRate(line.getGstRate())
                .taxableAmount(line.getTaxableAmount())
                .cgstAmount(line.getCgstAmount())
                .sgstAmount(line.getSgstAmount())
                .igstAmount(line.getIgstAmount())
                .lineTotal(line.getLineTotal())
                .sourceLineId(line.getSourceLineId())
                .build();
    }

    private Map<Long, String> warehouseNames() {
        return warehouseRepository.findByTenantIdOrderByNameAsc(tenantId()).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
    }

    private Map<Long, String> documentNumbers(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return documentRepository.findByTenantIdAndIdIn(tenantId(), ids).stream()
                .collect(Collectors.toMap(TradeDocument::getId, TradeDocument::getDocNumber));
    }

    // -- small helpers -------------------------------------------------------------------------

    public void markApproved(TradeDocument document) {
        document.setApprovedAt(Instant.now());
        document.setApprovedBy(currentUser());
    }

    public static String label(DocumentType type) {
        return switch (type) {
            case PURCHASE_ORDER -> "purchase order";
            case GOODS_RECEIPT -> "goods receipt";
            case PURCHASE_BILL -> "purchase bill";
            case DEBIT_NOTE -> "debit note";
            case QUOTATION -> "quotation";
            case SALES_ORDER -> "sales order";
            case DELIVERY_CHALLAN -> "delivery challan";
            case SALES_INVOICE -> "sales invoice";
            case CREDIT_NOTE -> "credit note";
        };
    }

    private static String humanStatus(DocumentStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
