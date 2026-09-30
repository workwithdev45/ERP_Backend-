package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgeingDocumentDto {
    private Long documentId;
    private String docNumber;
    private LocalDate docDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal balance;
    /** Negative when not yet due. */
    private long daysOverdue;
    private String bucket;
}
