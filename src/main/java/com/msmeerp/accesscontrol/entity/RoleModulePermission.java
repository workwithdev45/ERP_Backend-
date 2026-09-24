package com.msmeerp.accesscontrol.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A single module's action grant for one role, e.g. roleId=3, module=SALES,
 * actions={VIEW,CREATE}. This is the fine-grained layer beneath a Role —
 * an Admin assigns these per role, and every user in that role inherits them.
 */
@Entity
@Table(
        name = "role_module_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "module_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleModulePermission extends TenantAwareEntity {

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 30)
    private ModuleCode moduleCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "role_module_permission_actions",
            joinColumns = @JoinColumn(name = "role_module_permission_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    @Builder.Default
    private Set<PermissionAction> actions = new HashSet<>();
}
