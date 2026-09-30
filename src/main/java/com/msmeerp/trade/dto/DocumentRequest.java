package com.msmeerp.trade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequest {
    /** Required unless the document is created from a source document, whose party is reused. */
    private Long partyId;
    private LocalDate docDate;
    private LocalDate dueDate;
    private Long warehouseId;
    private Long sourceDocumentId;
    private String partyReference;
    private Boolean reverseCharge;
    private String notes;

    @Valid
    @NotEmpty(message = "Add at least one line")
    private List<DocumentLineRequest> lines;
}
