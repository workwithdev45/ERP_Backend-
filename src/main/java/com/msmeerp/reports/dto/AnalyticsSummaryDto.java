package com.msmeerp.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** W14 owner dashboard: this month's business, what's owed each way, stock, and open work. Sales/purchases exclude GST. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryDto {
    private BigDecimal salesThisMonth;
    private BigDecimal salesLastMonth;
    private BigDecimal purchasesThisMonth;
    private BigDecimal purchasesLastMonth;
    private BigDecimal receivedThisMonth;
    private BigDecimal paidThisMonth;

    private BigDecimal receivablesOutstanding;
    private BigDecimal receivablesOverdue;
    private int overdueInvoices;
    private BigDecimal payablesOutstanding;
    private BigDecimal payablesOverdue;
    private int overdueBills;

    private BigDecimal stockValue;
    private int lowStockItems;

    private int openSalesOrders;
    private BigDecimal undeliveredOrderValue;
    private int purchaseOrdersAwaitingApproval;
    private int openPurchaseOrders;

    /** Last six months, oldest first. */
    private List<MonthlyPointDto> trend;
    /** Last 90 days by sales value. */
    private List<RankedDto> topProducts;
    private List<RankedDto> topCustomers;
}
