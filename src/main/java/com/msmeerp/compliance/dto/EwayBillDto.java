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
public class EwayBillDto {
    private Long id;
    private String provider;
    private String ewbNo;
    private Instant ewbDate;
    private Instant validUntil;
    private String transportMode;
    private Integer distanceKm;
    private String vehicleNo;
    private String transporterId;
    private String transporterName;
    private String transportDocNo;
    private String status;
    private String cancelReason;
    private Instant cancelledAt;
    private Instant cancellableUntil;
}
