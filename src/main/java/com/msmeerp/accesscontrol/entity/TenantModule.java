package com.msmeerp.accesscontrol.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Whether one module is switched on for one tenant (G14) — replaces "every module is always
 * visible" with a per-company on/off toggle an Admin controls from Settings -> Modules.
 */
@Entity
@Table(
        name = "tenant_modules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "module_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantModule extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 30)
    private ModuleCode moduleCode;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
