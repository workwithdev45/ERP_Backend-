package com.msmeerp.compliance.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "eway_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EwayBill extends TenantAwareEntity {

    public enum Status { ACTIVE, CANCELLED }

    public enum TransportMode { ROAD, RAIL, AIR, SHIP }

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "ewb_no", nullable = false, length = 20)
    private String ewbNo;

    @Column(name = "ewb_date", nullable = false)
    private Instant ewbDate;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false, length = 10)
    private TransportMode transportMode;

    @Column(name = "distance_km", nullable = false)
    private Integer distanceKm;

    @Column(name = "vehicle_no", length = 20)
    private String vehicleNo;

    @Column(name = "transporter_id", length = 15)
    private String transporterId;

    @Column(name = "transporter_name", length = 100)
    private String transporterName;

    @Column(name = "transport_doc_no", length = 30)
    private String transportDocNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "cancel_reason", length = 150)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
