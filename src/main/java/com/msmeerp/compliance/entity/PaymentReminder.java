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

/** One payment reminder sent (or skipped) for an invoice. */
@Entity
@Table(name = "payment_reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReminder extends TenantAwareEntity {

    public enum Trigger { MANUAL, BEFORE_DUE, ON_DUE, OVERDUE }

    public enum Status { SENT, FAILED, SKIPPED }

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "recipient", length = 30)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private Trigger triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "error", length = 255)
    private String error;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
