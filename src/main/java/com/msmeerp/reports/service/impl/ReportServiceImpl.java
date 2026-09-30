package com.msmeerp.reports.service.impl;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.inventory.entity.InventoryItem;
import com.msmeerp.inventory.repository.InventoryItemRepository;
import com.msmeerp.reports.dto.AnalyticsSummaryDto;
import com.msmeerp.reports.dto.GstRateSummaryDto;
import com.msmeerp.reports.dto.MonthlyPointDto;
import com.msmeerp.reports.dto.RankedDto;
import com.msmeerp.reports.dto.RegisterDto;
import com.msmeerp.reports.dto.RegisterRowDto;
import com.msmeerp.reports.dto.StockValuationDto;
import com.msmeerp.reports.dto.StockValuationRowDto;
import com.msmeerp.reports.service.ReportService;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Payment;
import com.msmeerp.trade.entity.PaymentDirection;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import com.msmeerp.trade.repository.PaymentRepository;
import com.msmeerp.trade.repository.TradeDocumentLineRepository;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Set<DocumentType> SALES_TYPES = Set.of(DocumentType.SALES_INVOICE, DocumentType.CREDIT_NOTE);
    private static final Set<DocumentType> PURCHASE_TYPES = Set.of(DocumentType.PURCHASE_BILL, DocumentType.DEBIT_NOTE);
    private static final Set<DocumentStatus> OPEN_BILLING = Set.of(DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID);
    private static final int TREND_MONTHS = 6;
    private static final int TOP_DAYS = 90;
    private static final int TOP_N = 5;

    private final TradeDocumentRepository documentRepository;
    private final TradeDocumentLineRepository lineRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryItemRepository inventoryItemRepository;

    // -- dashboard -----------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryDto dashboard() {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth firstTrendMonth = thisMonth.minusMonths(TREND_MONTHS - 1L);

        // Sales/purchases per month over the trend window; this and last month are read off the same map.
        Map<YearMonth, BigDecimal[]> monthly = new TreeMap<>();
        for (int i = 0; i < TREND_MONTHS; i++) {
            monthly.put(firstTrendMonth.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        Set<DocumentType> tradeTypes = Set.of(DocumentType.SALES_INVOICE, DocumentType.CREDIT_NOTE, DocumentType.PURCHASE_BILL, DocumentType.DEBIT_NOTE);
        for (TradeDocument d : documentRepository.findByTenantIdAndDocTypeInAndDocDateBetweenOrderByDocDateAscIdAsc(
                tenantId, tradeTypes, firstTrendMonth.atDay(1), thisMonth.atEndOfMonth())) {
            if (d.getStatus() == DocumentStatus.CANCELLED) {
                continue;
            }
            BigDecimal[] point = monthly.get(YearMonth.from(d.getDocDate()));
            int slot = SALES_TYPES.contains(d.getDocType()) ? 0 : 1;
            point[slot] = point[slot].add(signed(d, d.getTaxableAmount()));
        }
        List<MonthlyPointDto> trend = monthly.entrySet().stream()
                .map(e -> MonthlyPointDto.builder().month(e.getKey().toString()).sales(e.getValue()[0]).purchases(e.getValue()[1]).build())
                .collect(Collectors.toList());

        BigDecimal received = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (Payment p : paymentRepository.findByTenantIdAndPaymentDateBetween(tenantId, thisMonth.atDay(1), today)) {
            if (p.getDirection() == PaymentDirection.RECEIVED) {
                received = received.add(p.getAmount());
            } else {
                paid = paid.add(p.getAmount());
            }
        }

        Outstanding receivables = outstanding(tenantId, DocumentType.SALES_INVOICE, today);
        Outstanding payables = outstanding(tenantId, DocumentType.PURCHASE_BILL, today);

        List<TradeDocument> openSalesOrders = documentRepository.findByTenantIdAndDocTypeInAndStatusIn(tenantId,
                Set.of(DocumentType.SALES_ORDER), Set.of(DocumentStatus.CONFIRMED, DocumentStatus.PARTIALLY_DELIVERED));
        List<TradeDocument> openPurchaseOrders = documentRepository.findByTenantIdAndDocTypeInAndStatusIn(tenantId,
                Set.of(DocumentType.PURCHASE_ORDER), Set.of(DocumentStatus.DRAFT, DocumentStatus.APPROVED, DocumentStatus.PARTIALLY_RECEIVED));

        List<InventoryItem> stock = inventoryItemRepository.findAllWithDetails(tenantId);
        BigDecimal stockValue = stock.stream().map(ReportServiceImpl::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add);

        return AnalyticsSummaryDto.builder()
                .salesThisMonth(monthly.get(thisMonth)[0])
                .salesLastMonth(monthly.get(thisMonth.minusMonths(1))[0])
                .purchasesThisMonth(monthly.get(thisMonth)[1])
                .purchasesLastMonth(monthly.get(thisMonth.minusMonths(1))[1])
                .receivedThisMonth(received)
                .paidThisMonth(paid)
                .receivablesOutstanding(receivables.total)
                .receivablesOverdue(receivables.overdue)
                .overdueInvoices(receivables.overdueCount)
                .payablesOutstanding(payables.total)
                .payablesOverdue(payables.overdue)
                .overdueBills(payables.overdueCount)
                .stockValue(stockValue.setScale(2, RoundingMode.HALF_UP))
                .lowStockItems(inventoryItemRepository.findLowStockItems(tenantId).size())
                .openSalesOrders(openSalesOrders.size())
                .undeliveredOrderValue(pendingValue(openSalesOrders))
                .purchaseOrdersAwaitingApproval((int) openPurchaseOrders.stream().filter(d -> d.getStatus() == DocumentStatus.DRAFT).count())
                .openPurchaseOrders(openPurchaseOrders.size())
                .trend(trend)
                .topProducts(topProducts(tenantId, today))
                .topCustomers(topCustomers(tenantId, today))
                .build();
    }

    private record Outstanding(BigDecimal total, BigDecimal overdue, int overdueCount) {
    }

    private Outstanding outstanding(String tenantId, DocumentType type, LocalDate today) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        int overdueCount = 0;
        for (TradeDocument d : documentRepository.findByTenantIdAndDocTypeInAndStatusIn(tenantId, Set.of(type), OPEN_BILLING)) {
            total = total.add(d.getBalance());
            LocalDate due = d.getDueDate() != null ? d.getDueDate() : d.getDocDate();
            if (due.isBefore(today)) {
                overdue = overdue.add(d.getBalance());
                overdueCount++;
            }
        }
        return new Outstanding(total, overdue, overdueCount);
    }

    private List<RankedDto> topProducts(String tenantId, LocalDate today) {
        Map<Long, RankedDto> byProduct = new HashMap<>();
        for (TradeDocumentLine line : lineRepository.findByDocumentTypesAndDateRange(tenantId, SALES_TYPES, today.minusDays(TOP_DAYS), today)) {
            int sign = line.getDocument().getDocType() == DocumentType.CREDIT_NOTE ? -1 : 1;
            RankedDto row = byProduct.computeIfAbsent(line.getProductId(), id -> RankedDto.builder()
                    .id(id).name(line.getProductName()).quantity(0).value(BigDecimal.ZERO).build());
            row.setQuantity(row.getQuantity() + sign * line.getQuantity());
            row.setValue(row.getValue().add(line.getTaxableAmount().multiply(BigDecimal.valueOf(sign))));
        }
        return top(byProduct.values());
    }

    private List<RankedDto> topCustomers(String tenantId, LocalDate today) {
        Map<Long, RankedDto> byCustomer = new HashMap<>();
        for (TradeDocument d : documentRepository.findByTenantIdAndDocTypeInAndDocDateBetweenOrderByDocDateAscIdAsc(
                tenantId, SALES_TYPES, today.minusDays(TOP_DAYS), today)) {
            RankedDto row = byCustomer.computeIfAbsent(d.getPartyId(), id -> RankedDto.builder()
                    .id(id).name(d.getPartyName()).value(BigDecimal.ZERO).build());
            row.setValue(row.getValue().add(signed(d, d.getTaxableAmount())));
        }
        return top(byCustomer.values());
    }

    private static List<RankedDto> top(Collection<RankedDto> rows) {
        return rows.stream()
                .filter(r -> r.getValue().signum() > 0)
                .sorted(Comparator.comparing(RankedDto::getValue).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    private static BigDecimal pendingValue(List<TradeDocument> orders) {
        BigDecimal value = BigDecimal.ZERO;
        for (TradeDocument order : orders) {
            for (TradeDocumentLine line : order.getLines()) {
                if (line.getPendingQuantity() > 0) {
                    value = value.add(line.getTaxableAmount().multiply(BigDecimal.valueOf(line.getPendingQuantity()))
                            .divide(BigDecimal.valueOf(line.getQuantity()), 2, RoundingMode.HALF_UP));
                }
            }
        }
        return value;
    }

    // -- registers -----------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public RegisterDto salesRegister(LocalDate from, LocalDate to) {
        return register(SALES_TYPES, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisterDto purchaseRegister(LocalDate from, LocalDate to) {
        return register(PURCHASE_TYPES, from, to);
    }

    private RegisterDto register(Set<DocumentType> types, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : YearMonth.now().atDay(1);
        LocalDate end = to != null ? to : LocalDate.now();
        if (end.isBefore(start)) {
            throw new BadRequestException("The end date is before the start date");
        }
        String tenantId = TenantContext.getTenantId();

        List<TradeDocument> documents = documentRepository
                .findByTenantIdAndDocTypeInAndDocDateBetweenOrderByDocDateAscIdAsc(tenantId, types, start, end).stream()
                .filter(d -> d.getStatus() != DocumentStatus.CANCELLED)
                .collect(Collectors.toList());

        List<RegisterRowDto> rows = documents.stream().map(ReportServiceImpl::toRow).collect(Collectors.toList());
        RegisterRowDto totals = RegisterRowDto.builder()
                .taxableAmount(sum(rows, RegisterRowDto::getTaxableAmount))
                .cgstAmount(sum(rows, RegisterRowDto::getCgstAmount))
                .sgstAmount(sum(rows, RegisterRowDto::getSgstAmount))
                .igstAmount(sum(rows, RegisterRowDto::getIgstAmount))
                .roundOff(sum(rows, RegisterRowDto::getRoundOff))
                .totalAmount(sum(rows, RegisterRowDto::getTotalAmount))
                .build();

        // Rate-wise GST summary from the lines — the figures a GST return is filed from.
        Map<BigDecimal, GstRateSummaryDto> byRate = new TreeMap<>();
        for (TradeDocument d : documents) {
            for (TradeDocumentLine line : d.getLines()) {
                GstRateSummaryDto row = byRate.computeIfAbsent(line.getGstRate().stripTrailingZeros(), rate -> GstRateSummaryDto.builder()
                        .gstRate(rate).taxableAmount(BigDecimal.ZERO).cgstAmount(BigDecimal.ZERO)
                        .sgstAmount(BigDecimal.ZERO).igstAmount(BigDecimal.ZERO).build());
                row.setTaxableAmount(row.getTaxableAmount().add(signed(d, line.getTaxableAmount())));
                row.setCgstAmount(row.getCgstAmount().add(signed(d, line.getCgstAmount())));
                row.setSgstAmount(row.getSgstAmount().add(signed(d, line.getSgstAmount())));
                row.setIgstAmount(row.getIgstAmount().add(signed(d, line.getIgstAmount())));
            }
        }

        return RegisterDto.builder().from(start).to(end).rows(rows).totals(totals).byGstRate(new ArrayList<>(byRate.values())).build();
    }

    private static RegisterRowDto toRow(TradeDocument d) {
        return RegisterRowDto.builder()
                .documentId(d.getId())
                .docType(d.getDocType().name())
                .docNumber(d.getDocNumber())
                .docDate(d.getDocDate())
                .partyName(d.getPartyName())
                .partyGstin(d.getPartyGstin())
                .placeOfSupply(d.getPlaceOfSupply())
                .partyReference(d.getPartyReference())
                .status(d.getStatus().name())
                .reverseCharge(d.getReverseCharge())
                .taxableAmount(signed(d, d.getTaxableAmount()))
                .cgstAmount(signed(d, d.getCgstAmount()))
                .sgstAmount(signed(d, d.getSgstAmount()))
                .igstAmount(signed(d, d.getIgstAmount()))
                .roundOff(signed(d, d.getRoundOff()))
                .totalAmount(signed(d, d.getTotalAmount()))
                .build();
    }

    /** Credit and debit notes reduce sales and purchases, so they count negative. */
    private static BigDecimal signed(TradeDocument d, BigDecimal amount) {
        boolean note = d.getDocType() == DocumentType.CREDIT_NOTE || d.getDocType() == DocumentType.DEBIT_NOTE;
        return note ? amount.negate() : amount;
    }

    private static BigDecimal sum(List<RegisterRowDto> rows, Function<RegisterRowDto, BigDecimal> field) {
        return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -- stock valuation -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StockValuationDto stockValuation() {
        List<StockValuationRowDto> rows = inventoryItemRepository.findAllWithDetails(TenantContext.getTenantId()).stream()
                .filter(i -> i.getAvailableQuantity() != 0)
                .sorted(Comparator.comparing((InventoryItem i) -> i.getProduct().getName()).thenComparing(i -> i.getWarehouse().getName()))
                .map(i -> StockValuationRowDto.builder()
                        .productId(i.getProduct().getId())
                        .sku(i.getProduct().getSku())
                        .productName(i.getProduct().getName())
                        .category(i.getProduct().getCategory())
                        .warehouseName(i.getWarehouse().getName())
                        .uom(i.getProduct().getUnitOfMeasure())
                        .quantity(i.getAvailableQuantity())
                        .reserved(i.getReservedQuantity())
                        .averageCost(i.getAverageCost())
                        .value(stockValue(i).setScale(2, RoundingMode.HALF_UP))
                        .build())
                .collect(Collectors.toList());
        return StockValuationDto.builder()
                .rows(rows)
                .totalValue(rows.stream().map(StockValuationRowDto::getValue).reduce(BigDecimal.ZERO, BigDecimal::add))
                .rowsWithoutCost((int) rows.stream().filter(r -> r.getAverageCost() == null).count())
                .build();
    }

    private static BigDecimal stockValue(InventoryItem item) {
        return item.getAverageCost() == null
                ? BigDecimal.ZERO
                : item.getAverageCost().multiply(BigDecimal.valueOf(item.getAvailableQuantity()));
    }
}
