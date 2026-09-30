package com.msmeerp.trade.entity;

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

import java.math.BigDecimal;

/** W5 master: a customer and/or vendor. Its state drives GST place of supply. */
@Entity
@Table(name = "parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "payment_terms_days", nullable = false)
    @Builder.Default
    private Integer paymentTermsDays = 30;

    /** Customers only: sales orders and invoices are refused once outstanding + new would exceed this. */
    @Column(name = "credit_limit", precision = 16, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
