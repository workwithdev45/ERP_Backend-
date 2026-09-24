package com.msmeerp.accesscontrol.repository;

import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.RoleModulePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleModulePermissionRepository extends JpaRepository<RoleModulePermission, Long> {

    List<RoleModulePermission> findByTenantIdAndRoleId(String tenantId, Long roleId);

    List<RoleModulePermission> findByTenantIdAndRoleIdIn(String tenantId, Collection<Long> roleIds);

    Optional<RoleModulePermission> findByTenantIdAndRoleIdAndModuleCode(String tenantId, Long roleId, ModuleCode moduleCode);

    void deleteByTenantIdAndRoleIdAndModuleCode(String tenantId, Long roleId, ModuleCode moduleCode);
}
