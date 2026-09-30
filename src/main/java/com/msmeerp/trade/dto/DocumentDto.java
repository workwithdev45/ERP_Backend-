package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

/** Full document for the detail view: lines, follow-on documents and payments. */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentDto extends DocumentSummaryDto {
    private String partyGstin;
    private String placeOfSupply;
    private String notes;
    private String createdBy;
    private String approvedBy;
    private Instant approvedAt;
    private List<DocumentLineDto> lines;
    private List<DocumentSummaryDto> linkedDocuments;
    private List<AllocationDto> payments;
}
