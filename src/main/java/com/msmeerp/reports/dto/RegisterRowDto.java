package com.msmeerp.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One invoice/bill (positive) or credit/debit note (negative) in a sales or purchase register. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRowDto {
    private Long documentId;
    private String docType;
    private String docNumber;
    private LocalDate docDate;
    private String partyName;
    private String partyGstin;
    private String placeOfSupply;
    private String partyReference;
    private String status;
    private Boolean reverseCharge;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal roundOff;
    private BigDecimal totalAmount;
}
