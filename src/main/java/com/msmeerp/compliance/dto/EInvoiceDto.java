package com.msmeerp.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EInvoiceDto {
    private Long id;
    private String provider;
    private String irn;
    private String ackNo;
    private Instant ackDate;
    private String signedQr;
    private String status;
    private String cancelReason;
    private Instant cancelledAt;
    /** Until when the IRN can still be cancelled (24 hours after acknowledgement). */
    private Instant cancellableUntil;
}
