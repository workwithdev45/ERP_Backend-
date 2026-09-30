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

/** The IRN, acknowledgement and signed QR the IRP issued for one sales invoice. */
@Entity
@Table(name = "einvoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EInvoice extends TenantAwareEntity {

    public enum Status { GENERATED, CANCELLED }

    @Column(name = "document_id", nullable = false, unique = true)
    private Long documentId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "irn", nullable = false, length = 64)
    private String irn;

    @Column(name = "ack_no", nullable = false, length = 20)
    private String ackNo;

    @Column(name = "ack_date", nullable = false)
    private Instant ackDate;

    /** Signed QR payload (a JWT) — printed as the QR code on the invoice. */
    @Column(name = "signed_qr", nullable = false, columnDefinition = "TEXT")
    private String signedQr;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "cancel_reason", length = 150)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** The e-invoice JSON (INV-01 schema) sent to the IRP, kept for audit. */
    @Column(name = "request_json", nullable = false, columnDefinition = "TEXT")
    private String requestJson;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
