package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentSummaryDto {
    private Long id;
    private String docType;
    private String docNumber;
    private String status;
    private Long partyId;
    private String partyName;
    private LocalDate docDate;
    private LocalDate dueDate;
    private Long warehouseId;
    private String warehouseName;
    private Long sourceDocumentId;
    private String sourceDocumentNumber;
    private String partyReference;
    private Boolean interState;
    private Boolean reverseCharge;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal roundOff;
    private BigDecimal totalAmount;
    private BigDecimal settledAmount;
    private BigDecimal balance;
}
