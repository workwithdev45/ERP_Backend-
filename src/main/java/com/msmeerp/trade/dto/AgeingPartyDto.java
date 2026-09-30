package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Receivables/payables ageing for one party, bucketed by days past due date. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgeingPartyDto {
    private Long partyId;
    private String partyName;
    private BigDecimal notDue;
    private BigDecimal days1To30;
    private BigDecimal days31To60;
    private BigDecimal days61To90;
    private BigDecimal over90;
    private BigDecimal total;
    private List<AgeingDocumentDto> documents;
}
