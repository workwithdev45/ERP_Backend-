package com.msmeerp.accesscontrol.repository;

import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.TenantModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantModuleRepository extends JpaRepository<TenantModule, Long> {

    List<TenantModule> findByTenantId(String tenantId);

    Optional<TenantModule> findByTenantIdAndModuleCode(String tenantId, ModuleCode moduleCode);

    boolean existsByTenantIdAndModuleCodeAndEnabledTrue(String tenantId, ModuleCode moduleCode);
}
