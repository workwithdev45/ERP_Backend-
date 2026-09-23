package com.msmeerp.tenant.entity;

import com.msmeerp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_tenant_map")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTenantMap extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "tenant_ids", nullable = false, columnDefinition = "TEXT")
    private String tenantIds; // Comma-separated list of tenant portal IDs
}
