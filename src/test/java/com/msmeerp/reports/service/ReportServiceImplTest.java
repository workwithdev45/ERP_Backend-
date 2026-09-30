package com.msmeerp.reports.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.inventory.entity.ItemType;
import com.msmeerp.inventory.repository.InventoryItemRepository;
import com.msmeerp.reports.dto.RegisterDto;
import com.msmeerp.reports.service.impl.ReportServiceImpl;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import com.msmeerp.trade.repository.PaymentRepository;
import com.msmeerp.trade.repository.TradeDocumentLineRepository;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private TradeDocumentRepository documentRepository;
    @Mock
    private TradeDocumentLineRepository lineRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static TradeDocument doc(DocumentType type, DocumentStatus status, String taxable, String igst, String total, String gstRate) {
        TradeDocument document = TradeDocument.builder().docType(type).docNumber(type.getPrefix() + "-1").status(status)
                .partyName("Karnataka Irrigation Supplies").docDate(LocalDate.of(2026, 9, 30)).interState(true)
                .taxableAmount(new BigDecimal(taxable)).igstAmount(new BigDecimal(igst)).totalAmount(new BigDecimal(total))
                .build();
        document.addLine(TradeDocumentLine.builder().productId(3L).productName("Pump").itemType(ItemType.STOCK).quantity(1)
                .rate(new BigDecimal(taxable)).gstRate(new BigDecimal(gstRate))
                .taxableAmount(new BigDecimal(taxable)).igstAmount(new BigDecimal(igst)).build());
        return document;
    }

    @Test
    void salesRegister_countsCreditNotesNegativeAndSkipsCancelled() {
        when(documentRepository.findByTenantIdAndDocTypeInAndDocDateBetweenOrderByDocDateAscIdAsc(eq(TENANT_ID), any(), any(), any()))
                .thenReturn(List.of(
                        doc(DocumentType.SALES_INVOICE, DocumentStatus.PARTIALLY_PAID, "15630.00", "2813.40", "18443.00", "18"),
                        doc(DocumentType.CREDIT_NOTE, DocumentStatus.POSTED, "4410.00", "793.80", "5204.00", "18"),
                        doc(DocumentType.SALES_INVOICE, DocumentStatus.CANCELLED, "999.00", "0", "999.00", "18")));

        RegisterDto register = reportService.salesRegister(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(register.getRows()).hasSize(2);
        assertThat(register.getRows().get(1).getTotalAmount()).isEqualByComparingTo("-5204.00");
        assertThat(register.getTotals().getTaxableAmount()).isEqualByComparingTo("11220.00");
        assertThat(register.getTotals().getIgstAmount()).isEqualByComparingTo("2019.60");
        assertThat(register.getByGstRate()).singleElement()
                .satisfies(rate -> assertThat(rate.getTaxableAmount()).isEqualByComparingTo("11220.00"));
    }

    @Test
    void register_rejectsAnEndDateBeforeTheStart() {
        assertThatThrownBy(() -> reportService.purchaseRegister(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(BadRequestException.class);
    }
}
