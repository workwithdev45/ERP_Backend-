package com.msmeerp.accesscontrol.repository;

import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.UserModulePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserModulePermissionRepository extends JpaRepository<UserModulePermission, Long> {

    List<UserModulePermission> findByTenantIdAndUserId(String tenantId, Long userId);

    Optional<UserModulePermission> findByTenantIdAndUserIdAndModuleCode(String tenantId, Long userId, ModuleCode moduleCode);

    void deleteByTenantIdAndUserIdAndModuleCode(String tenantId, Long userId, ModuleCode moduleCode);
}
