package com.msmeerp.reports.service;

import com.msmeerp.reports.dto.AnalyticsSummaryDto;
import com.msmeerp.reports.dto.RegisterDto;
import com.msmeerp.reports.dto.StockValuationDto;

import java.time.LocalDate;

/** W14: owner dashboard and standard reports. Receivables/payables ageing live in Sales/Purchase. */
public interface ReportService {
    AnalyticsSummaryDto dashboard();

    /** Invoices and credit notes dated between {@code from} and {@code to}, inclusive. */
    RegisterDto salesRegister(LocalDate from, LocalDate to);

    /** Bills and debit notes dated between {@code from} and {@code to}, inclusive. */
    RegisterDto purchaseRegister(LocalDate from, LocalDate to);

    StockValuationDto stockValuation();
}
