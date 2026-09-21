package com.hms.tenant.repository;

import com.hms.tenant.entity.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Long> {
    List<TenantSettings> findByTenantId(String tenantId);
    Optional<TenantSettings> findByTenantIdAndKey(String tenantId, String key);
}
