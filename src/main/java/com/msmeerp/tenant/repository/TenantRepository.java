package com.msmeerp.tenant.repository;

import com.msmeerp.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findBySubdomain(String subdomain);
    Optional<Tenant> findByPortalId(String portalId);
    Optional<Tenant> findByAdminEmail(String adminEmail);
    boolean existsByIdAndActiveTrue(String id);
    boolean existsByPortalId(String portalId);
    boolean existsByPortalIdAndActiveTrue(String portalId);
    boolean existsByAdminEmailAndActiveTrue(String adminEmail);
}
