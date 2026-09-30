package com.msmeerp.trade.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A customer receipt or vendor payment, allocated against one or more invoices/bills. */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends TenantAwareEntity {

    @Column(name = "payment_number", nullable = false, length = 30)
    private String paymentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private PaymentDirection direction;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "party_name", nullable = false, length = 150)
    private String partyName;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private PaymentMode mode;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentAllocation> allocations = new ArrayList<>();
}
