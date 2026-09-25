package com.msmeerp.tenant.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenants")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant implements Persistable<String> {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "subdomain", unique = true, length = 50)
    private String subdomain;

    @Column(name = "portal_id", unique = true, length = 50)
    private String portalId;

    @Column(name = "database_name", length = 100)
    private String databaseName;

    @Column(name = "admin_email", length = 150)
    private String adminEmail;

    @Column(name = "subscription_tier", length = 50)
    @Builder.Default
    private String subscriptionTier = "Standard";

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "locale", length = 20)
    @Builder.Default
    private String locale = "en_IN";

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "first_login_pending")
    @Builder.Default
    private boolean firstLoginPending = true;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    // G3: company/GST details needed to produce a correct invoice — captured during onboarding
    // and editable later from Settings -> Company.
    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    /** 1-12 — the calendar month an Indian financial year starts in (April = 4 by default). */
    @Column(name = "financial_year_start_month", nullable = false)
    @Builder.Default
    private int financialYearStartMonth = 4;

    /** G10: Trader / Manufacturer / Services — drives which modules/presets onboarding suggests. */
    @Column(name = "business_type", length = 30)
    private String businessType;

    // G11: Terms & Privacy acceptance, captured at signup.
    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "terms_version", length = 20)
    private String termsVersion;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TenantSettings> settings = new ArrayList<>();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    @LastModifiedDate
    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}

