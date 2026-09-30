package com.msmeerp.trade.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.inventory.entity.ItemType;
import com.msmeerp.inventory.entity.StockMovement;
import com.msmeerp.inventory.service.InventoryService;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.trade.dto.DocumentLineRequest;
import com.msmeerp.trade.dto.DocumentRequest;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.PartyType;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private DocumentEngine engine;
    @Mock
    private PaymentEngine paymentEngine;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private TradeDocumentRepository documentRepository;

    @InjectMocks
    private SalesServiceImpl salesService;

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static Party customer(String creditLimit) {
        Party party = Party.builder().partyType(PartyType.CUSTOMER).name("Nashik Farm Equipments")
                .creditLimit(creditLimit == null ? null : new BigDecimal(creditLimit)).paymentTermsDays(21).build();
        party.setId(5L);
        return party;
    }

    private static TradeDocumentLine stockLine(long id, int qty, int reserved) {
        TradeDocumentLine line = TradeDocumentLine.builder().productId(3L).productName("Pump").itemType(ItemType.STOCK)
                .quantity(qty).reservedQuantity(reserved).rate(new BigDecimal("4800")).lineTotal(new BigDecimal("4800").multiply(BigDecimal.valueOf(qty)))
                .build();
        line.setId(id);
        return line;
    }

    @Test
    void createSalesOrder_refusesAnOrderThatWouldPassTheCreditLimit() {
        Party customer = customer("25000");
        TradeDocument order = TradeDocument.builder().docType(DocumentType.SALES_ORDER).docNumber("SO-0003")
                .status(DocumentStatus.CONFIRMED).warehouseId(1L).totalAmount(new BigDecimal("27895.20")).build();
        when(engine.tenantId()).thenReturn(TENANT_ID);
        when(engine.requireCustomer(5L)).thenReturn(customer);
        when(engine.newDocument(eq(DocumentType.SALES_ORDER), eq(DocumentStatus.CONFIRMED), eq(customer), any())).thenReturn(order);
        when(engine.lineFromProduct(any())).thenReturn(stockLine(11L, 4, 0));
        when(documentRepository.findByTenantIdAndDocTypeAndPartyIdAndStatusIn(eq(TENANT_ID), any(), eq(5L), any())).thenReturn(List.of());

        DocumentRequest request = DocumentRequest.builder().partyId(5L).warehouseId(1L)
                .lines(List.of(DocumentLineRequest.builder().productId(3L).quantity(4).rate(new BigDecimal("4800")).build())).build();

        assertThatThrownBy(() -> salesService.createSalesOrder(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Credit limit exceeded");
        verify(documentRepository, never()).save(any());
        verify(inventoryService, never()).reserveAvailable(anyLong(), anyLong(), anyInt());
    }

    @Test
    void createDeliveryChallan_shipsFromTheOrdersReservationFirstAndMarksItPartiallyDelivered() {
        TradeDocumentLine orderLine = stockLine(11L, 4, 4);
        TradeDocument order = TradeDocument.builder().docType(DocumentType.SALES_ORDER).docNumber("SO-0003")
                .status(DocumentStatus.CONFIRMED).partyId(5L).warehouseId(1L).build();
        order.addLine(orderLine);
        TradeDocument challan = TradeDocument.builder().docType(DocumentType.DELIVERY_CHALLAN).docNumber("DC-0002")
                .status(DocumentStatus.POSTED).partyName("Nashik Farm Equipments").build();

        when(engine.requireDocument(20L, DocumentType.SALES_ORDER)).thenReturn(order);
        when(engine.pickSourceLines(eq(order), any(), any())).thenReturn(Map.of(orderLine, 2));
        when(engine.getParty(5L)).thenReturn(customer(null));
        when(engine.newDocument(eq(DocumentType.DELIVERY_CHALLAN), eq(DocumentStatus.POSTED), any(), any())).thenReturn(challan);
        when(engine.lineFromSource(orderLine, 2)).thenReturn(stockLine(12L, 2, 0));

        DocumentRequest request = DocumentRequest.builder().sourceDocumentId(20L)
                .lines(List.of(DocumentLineRequest.builder().sourceLineId(11L).quantity(2).build())).build();
        salesService.createDeliveryChallan(request);

        InOrder stock = inOrder(inventoryService);
        stock.verify(inventoryService).releaseReserved(3L, 1L, 2);
        stock.verify(inventoryService).issueForDocument(eq(3L), eq(1L), eq(2), eq(StockMovement.MovementType.OUT),
                eq("DELIVERY"), eq("DC-0002"), any());
        assertThat(orderLine.getReservedQuantity()).isEqualTo(2);
        assertThat(orderLine.getFulfilledQuantity()).isEqualTo(2);
        assertThat(order.getStatus()).isEqualTo(DocumentStatus.PARTIALLY_DELIVERED);
    }

    @Test
    void cancelSalesOrder_releasesEveryReservation() {
        TradeDocumentLine orderLine = stockLine(11L, 700, 600);
        TradeDocument order = TradeDocument.builder().docType(DocumentType.SALES_ORDER).docNumber("SO-0002")
                .status(DocumentStatus.CONFIRMED).warehouseId(1L).build();
        order.addLine(orderLine);
        when(engine.requireDocument(13L, DocumentType.SALES_ORDER)).thenReturn(order);
        when(documentRepository.save(order)).thenReturn(order);

        salesService.cancelSalesOrder(13L);

        verify(inventoryService).releaseReserved(3L, 1L, 600);
        assertThat(orderLine.getReservedQuantity()).isZero();
        assertThat(order.getStatus()).isEqualTo(DocumentStatus.CANCELLED);
    }
}
