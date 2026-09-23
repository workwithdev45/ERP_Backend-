package com.msmeerp.tenant.entity;

import com.msmeerp.common.entity.BaseEntity;
import com.msmeerp.tenant.context.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @PrePersist
    public void prePersistTenant() {
        if (this.tenantId == null) {
            String current = TenantContext.getTenantId();
            this.tenantId = current != null ? current : "msmeerp-main";
        }
    }

    @PreUpdate
    public void preUpdateTenant() {
        if (this.tenantId == null) {
            String current = TenantContext.getTenantId();
            this.tenantId = current != null ? current : "msmeerp-main";
        }
    }
}
