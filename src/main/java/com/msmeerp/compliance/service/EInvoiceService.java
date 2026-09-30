package com.msmeerp.compliance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.util.SecurityUtils;
import com.msmeerp.compliance.dto.CancelRequest;
import com.msmeerp.compliance.dto.ComplianceDto;
import com.msmeerp.compliance.dto.EInvoiceDto;
import com.msmeerp.compliance.dto.EwayBillDto;
import com.msmeerp.compliance.dto.EwayBillRequest;
import com.msmeerp.compliance.entity.EInvoice;
import com.msmeerp.compliance.entity.EwayBill;
import com.msmeerp.compliance.gsp.GspClient;
import com.msmeerp.compliance.repository.EInvoiceRepository;
import com.msmeerp.compliance.repository.EwayBillRepository;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import com.msmeerp.trade.repository.PartyRepository;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * W13: e-invoice (IRN + signed QR) and e-way bill for sales invoices, through the configured
 * {@link GspClient}. Builds the IRP's INV-01 JSON from the invoice, company and customer.
 */
@Service
@RequiredArgsConstructor
public class EInvoiceService {

    static final Duration CANCEL_WINDOW = Duration.ofHours(24);
    static final BigDecimal EWAY_BILL_THRESHOLD = new BigDecimal("50000");
    private static final DateTimeFormatter DOC_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GspClient gsp;
    private final EInvoiceRepository einvoiceRepository;
    private final EwayBillRepository ewayBillRepository;
    private final TradeDocumentRepository documentRepository;
    private final PartyRepository partyRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ComplianceDto status(Long invoiceId) {
        TradeDocument invoice = requireInvoice(invoiceId);
        String tenantId = TenantContext.getTenantId();
        EInvoice einvoice = einvoiceRepository.findByTenantIdAndDocumentId(tenantId, invoiceId).orElse(null);
        List<EwayBill> ewayBills = ewayBillRepository.findByTenantIdAndDocumentIdOrderByIdDesc(tenantId, invoiceId);
        BigDecimal goodsValue = goodsValue(invoice);
        return ComplianceDto.builder()
                .documentId(invoiceId)
                .einvoice(einvoice == null ? null : toDto(einvoice))
                .ewayBills(ewayBills.stream().map(this::toDto).collect(Collectors.toList()))
                .einvoiceBlockedReason(einvoice != null ? null : einvoiceBlocker(invoice))
                .ewayBillBlockedReason(ewayBillBlocker(invoice, ewayBills))
                .goodsValue(goodsValue)
                .ewayBillRequired(goodsValue.compareTo(EWAY_BILL_THRESHOLD) > 0)
                .build();
    }

    // -- e-invoice -----------------------------------------------------------------------------

    @Transactional
    public ComplianceDto generateIrn(Long invoiceId) {
        TradeDocument invoice = requireInvoice(invoiceId);
        String tenantId = TenantContext.getTenantId();
        if (einvoiceRepository.findByTenantIdAndDocumentId(tenantId, invoiceId).isPresent()) {
            throw new BadRequestException("2150: An IRN already exists for " + invoice.getDocNumber());
        }
        String blocker = einvoiceBlocker(invoice);
        if (blocker != null) {
            throw new BadRequestException(blocker);
        }

        Map<String, Object> payload = buildEinvoice(invoice, company(), party(invoice));
        GspClient.IrnResult result = gsp.generateIrn(payload);
        if (einvoiceRepository.existsByIrn(result.irn())) {
            throw new BadRequestException("2150: Duplicate IRN — this document number was already registered");
        }

        EInvoice einvoice = EInvoice.builder()
                .documentId(invoiceId)
                .provider(gsp.name())
                .irn(result.irn())
                .ackNo(result.ackNo())
                .ackDate(result.ackDate())
                .signedQr(result.signedQr())
                .status(EInvoice.Status.GENERATED)
                .requestJson(toJson(payload))
                .createdBy(currentUser())
                .build();
        einvoice.setTenantId(tenantId);
        einvoiceRepository.save(einvoice);
        return status(invoiceId);
    }

    @Transactional
    public ComplianceDto cancelIrn(Long invoiceId, CancelRequest request) {
        requireInvoice(invoiceId);
        String tenantId = TenantContext.getTenantId();
        EInvoice einvoice = einvoiceRepository.findByTenantIdAndDocumentId(tenantId, invoiceId)
                .orElseThrow(() -> new BadRequestException("There's no IRN to cancel"));
        if (einvoice.getStatus() == EInvoice.Status.CANCELLED) {
            throw new BadRequestException("The IRN is already cancelled");
        }
        if (Instant.now().isAfter(einvoice.getAckDate().plus(CANCEL_WINDOW))) {
            throw new BadRequestException("An IRN can only be cancelled within 24 hours — issue a credit note instead");
        }
        boolean activeEwayBill = ewayBillRepository.findByTenantIdAndDocumentIdOrderByIdDesc(tenantId, invoiceId).stream()
                .anyMatch(e -> e.getStatus() == EwayBill.Status.ACTIVE);
        if (activeEwayBill) {
            throw new BadRequestException("Cancel the active e-way bill first");
        }
        gsp.cancelIrn(einvoice.getIrn(), request.getReasonCode(), request.getRemark());
        einvoice.setStatus(EInvoice.Status.CANCELLED);
        einvoice.setCancelReason(request.getRemark().trim());
        einvoice.setCancelledAt(Instant.now());
        einvoiceRepository.save(einvoice);
        return status(invoiceId);
    }

    // -- e-way bill ----------------------------------------------------------------------------

    @Transactional
    public ComplianceDto generateEwayBill(Long invoiceId, EwayBillRequest request) {
        TradeDocument invoice = requireInvoice(invoiceId);
        String tenantId = TenantContext.getTenantId();
        String blocker = ewayBillBlocker(invoice, ewayBillRepository.findByTenantIdAndDocumentIdOrderByIdDesc(tenantId, invoiceId));
        if (blocker != null) {
            throw new BadRequestException(blocker);
        }
        Tenant company = company();
        Party customer = party(invoice);
        if (!StringUtils.hasText(customer.getPincode())) {
            throw new BadRequestException("Add " + customer.getName() + "'s PIN code in Sales → Customers — the e-way bill needs it");
        }

        String vehicleNo = normalise(request.getVehicleNo());
        String transporterId = normalise(request.getTransporterId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("Gstin", company.getGstin());
        payload.put("DocType", "INV");
        payload.put("DocNo", invoice.getDocNumber());
        payload.put("DocDate", invoice.getDocDate().format(DOC_DATE));
        payload.put("FromGstin", company.getGstin());
        payload.put("FromPincode", company.getPincode());
        payload.put("ToGstin", StringUtils.hasText(customer.getGstin()) ? customer.getGstin() : "URP");
        payload.put("ToPincode", customer.getPincode());
        payload.put("TotInvValue", invoice.getTotalAmount());
        payload.put("TransMode", String.valueOf(request.getTransportMode().ordinal() + 1));
        payload.put("Distance", request.getDistanceKm());
        payload.put("VehNo", vehicleNo);
        payload.put("TransId", transporterId);
        payload.put("TransName", trim(request.getTransporterName()));
        payload.put("TransDocNo", trim(request.getTransportDocNo()));
        einvoiceRepository.findByTenantIdAndDocumentId(tenantId, invoiceId)
                .filter(e -> e.getStatus() == EInvoice.Status.GENERATED)
                .ifPresent(e -> payload.put("Irn", e.getIrn()));

        GspClient.EwayBillResult result = gsp.generateEwayBill(payload);
        EwayBill ewayBill = EwayBill.builder()
                .documentId(invoiceId)
                .provider(gsp.name())
                .ewbNo(result.ewbNo())
                .ewbDate(result.ewbDate())
                .validUntil(result.validUntil())
                .transportMode(request.getTransportMode())
                .distanceKm(request.getDistanceKm())
                .vehicleNo(vehicleNo)
                .transporterId(transporterId)
                .transporterName(trim(request.getTransporterName()))
                .transportDocNo(trim(request.getTransportDocNo()))
                .status(EwayBill.Status.ACTIVE)
                .createdBy(currentUser())
                .build();
        ewayBill.setTenantId(tenantId);
        ewayBillRepository.save(ewayBill);
        return status(invoiceId);
    }

    @Transactional
    public ComplianceDto cancelEwayBill(Long ewayBillId, CancelRequest request) {
        EwayBill ewayBill = ewayBillRepository.findByTenantIdAndId(TenantContext.getTenantId(), ewayBillId)
                .orElseThrow(() -> new ResourceNotFoundException("E-way bill not found with id: " + ewayBillId));
        if (ewayBill.getStatus() == EwayBill.Status.CANCELLED) {
            throw new BadRequestException("The e-way bill is already cancelled");
        }
        if (Instant.now().isAfter(ewayBill.getEwbDate().plus(CANCEL_WINDOW))) {
            throw new BadRequestException("An e-way bill can only be cancelled within 24 hours of generation");
        }
        gsp.cancelEwayBill(ewayBill.getEwbNo(), request.getReasonCode(), request.getRemark());
        ewayBill.setStatus(EwayBill.Status.CANCELLED);
        ewayBill.setCancelReason(request.getRemark().trim());
        ewayBill.setCancelledAt(Instant.now());
        ewayBillRepository.save(ewayBill);
        return status(ewayBill.getDocumentId());
    }

    // -- rules ---------------------------------------------------------------------------------

    private String einvoiceBlocker(TradeDocument invoice) {
        Tenant company = company();
        Party customer = party(invoice);
        if (!StringUtils.hasText(company.getGstin())) {
            return "Add your company's GSTIN in Settings → Company to e-invoice";
        }
        if (!StringUtils.hasText(customer.getGstin())) {
            return "E-invoicing covers B2B supplies only — " + customer.getName() + " has no GSTIN";
        }
        if (!StringUtils.hasText(company.getAddressLine1()) || !StringUtils.hasText(company.getCity()) || !StringUtils.hasText(company.getPincode())) {
            return "Complete your company's address, city and PIN code in Settings → Company";
        }
        if (!StringUtils.hasText(customer.getAddressLine1()) || !StringUtils.hasText(customer.getCity()) || !StringUtils.hasText(customer.getPincode())) {
            return "Add " + customer.getName() + "'s address, city and PIN code in Sales → Customers — the IRP requires them";
        }
        List<String> missingHsn = invoice.getLines().stream()
                .filter(l -> !StringUtils.hasText(l.getHsnCode()))
                .map(TradeDocumentLine::getProductName)
                .collect(Collectors.toList());
        if (!missingHsn.isEmpty()) {
            return "Every item needs an HSN/SAC code — missing on " + String.join(", ", missingHsn);
        }
        return null;
    }

    private String ewayBillBlocker(TradeDocument invoice, List<EwayBill> existing) {
        if (goodsValue(invoice).signum() == 0) {
            return "No goods move on this invoice — e-way bills are for goods only";
        }
        if (existing.stream().anyMatch(e -> e.getStatus() == EwayBill.Status.ACTIVE)) {
            return "An e-way bill is already active for this invoice";
        }
        if (!StringUtils.hasText(company().getGstin())) {
            return "Add your company's GSTIN in Settings → Company to generate e-way bills";
        }
        return null;
    }

    private static BigDecimal goodsValue(TradeDocument invoice) {
        return invoice.getLines().stream()
                .filter(TradeDocumentLine::isStockItem)
                .map(l -> l.getTaxableAmount().add(l.getCgstAmount()).add(l.getSgstAmount()).add(l.getIgstAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -- INV-01 payload ------------------------------------------------------------------------

    Map<String, Object> buildEinvoice(TradeDocument invoice, Tenant company, Party customer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("Version", "1.1");
        payload.put("TranDtls", Map.of("TaxSch", "GST", "SupTyp", "B2B", "RegRev", "N", "IgstOnIntra", "N"));
        payload.put("DocDtls", Map.of("Typ", "INV", "No", invoice.getDocNumber(), "Dt", invoice.getDocDate().format(DOC_DATE)));

        Map<String, Object> seller = new LinkedHashMap<>();
        seller.put("Gstin", company.getGstin());
        seller.put("LglNm", StringUtils.hasText(company.getLegalName()) ? company.getLegalName() : company.getName());
        seller.put("Addr1", trim(company.getAddressLine1()));
        seller.put("Loc", trim(company.getCity()));
        seller.put("Pin", trim(company.getPincode()));
        seller.put("Stcd", company.getGstin().substring(0, 2));
        payload.put("SellerDtls", seller);

        Map<String, Object> buyer = new LinkedHashMap<>();
        buyer.put("Gstin", customer.getGstin());
        buyer.put("LglNm", customer.getName());
        buyer.put("Pos", customer.getGstin().substring(0, 2));
        buyer.put("Addr1", trim(customer.getAddressLine1()));
        buyer.put("Loc", trim(customer.getCity()));
        buyer.put("Pin", trim(customer.getPincode()));
        buyer.put("Stcd", customer.getGstin().substring(0, 2));
        payload.put("BuyerDtls", buyer);

        List<Map<String, Object>> items = new ArrayList<>();
        for (TradeDocumentLine line : invoice.getLines()) {
            BigDecimal gross = line.getRate().multiply(BigDecimal.valueOf(line.getQuantity()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("SlNo", String.valueOf(line.getLineNo()));
            item.put("PrdDesc", line.getProductName());
            item.put("IsServc", line.isStockItem() ? "N" : "Y");
            item.put("HsnCd", line.getHsnCode());
            item.put("Qty", line.getQuantity());
            item.put("Unit", uqc(line.getUom()));
            item.put("UnitPrice", line.getRate());
            item.put("TotAmt", gross);
            item.put("Discount", gross.subtract(line.getTaxableAmount()));
            item.put("AssAmt", line.getTaxableAmount());
            item.put("GstRt", line.getGstRate());
            item.put("IgstAmt", line.getIgstAmount());
            item.put("CgstAmt", line.getCgstAmount());
            item.put("SgstAmt", line.getSgstAmount());
            item.put("TotItemVal", line.getLineTotal());
            items.add(item);
        }
        payload.put("ItemList", items);

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("AssVal", invoice.getTaxableAmount());
        values.put("CgstVal", invoice.getCgstAmount());
        values.put("SgstVal", invoice.getSgstAmount());
        values.put("IgstVal", invoice.getIgstAmount());
        values.put("RndOffAmt", invoice.getRoundOff());
        values.put("TotInvVal", invoice.getTotalAmount());
        payload.put("ValDtls", values);
        return payload;
    }

    /** Unit quantity codes the IRP accepts; anything unrecognised is OTH. */
    private static String uqc(String uom) {
        if (uom == null) return "OTH";
        return switch (uom.trim().toUpperCase(Locale.ROOT)) {
            case "PCS", "PC", "PIECE", "PIECES" -> "PCS";
            case "NOS", "NO", "NUMBER", "NUMBERS" -> "NOS";
            case "SET", "SETS" -> "SET";
            case "BOX", "BOXES" -> "BOX";
            case "KG", "KGS" -> "KGS";
            case "LTR", "LITRE", "LITER" -> "LTR";
            case "MTR", "METRE", "METER" -> "MTR";
            default -> "OTH";
        };
    }

    // -- helpers -------------------------------------------------------------------------------

    private TradeDocument requireInvoice(Long invoiceId) {
        TradeDocument document = documentRepository.findByTenantIdAndId(TenantContext.getTenantId(), invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        if (document.getDocType() != DocumentType.SALES_INVOICE || document.getStatus() == DocumentStatus.CANCELLED) {
            throw new BadRequestException(document.getDocNumber() + " is not an active sales invoice");
        }
        return document;
    }

    private Tenant company() {
        return tenantRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    private Party party(TradeDocument invoice) {
        return partyRepository.findByTenantIdAndId(TenantContext.getTenantId(), invoice.getPartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private EInvoiceDto toDto(EInvoice e) {
        return EInvoiceDto.builder()
                .id(e.getId())
                .provider(e.getProvider())
                .irn(e.getIrn())
                .ackNo(e.getAckNo())
                .ackDate(e.getAckDate())
                .signedQr(e.getSignedQr())
                .status(e.getStatus().name())
                .cancelReason(e.getCancelReason())
                .cancelledAt(e.getCancelledAt())
                .cancellableUntil(e.getAckDate().plus(CANCEL_WINDOW))
                .build();
    }

    private EwayBillDto toDto(EwayBill e) {
        return EwayBillDto.builder()
                .id(e.getId())
                .provider(e.getProvider())
                .ewbNo(e.getEwbNo())
                .ewbDate(e.getEwbDate())
                .validUntil(e.getValidUntil())
                .transportMode(e.getTransportMode().name())
                .distanceKm(e.getDistanceKm())
                .vehicleNo(e.getVehicleNo())
                .transporterId(e.getTransporterId())
                .transporterName(e.getTransporterName())
                .transportDocNo(e.getTransportDocNo())
                .status(e.getStatus().name())
                .cancelReason(e.getCancelReason())
                .cancelledAt(e.getCancelledAt())
                .cancellableUntil(e.getEwbDate().plus(CANCEL_WINDOW))
                .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String currentUser() {
        return SecurityUtils.getCurrentUsername().orElse("system");
    }

    private static String normalise(String value) {
        return StringUtils.hasText(value) ? value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT) : null;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
