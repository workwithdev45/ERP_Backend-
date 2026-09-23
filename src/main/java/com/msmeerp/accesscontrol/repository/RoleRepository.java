package com.msmeerp.accesscontrol.repository;

import com.msmeerp.accesscontrol.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByTenantId(String tenantId);
    Optional<Role> findByTenantIdAndName(String tenantId, String name);
    boolean existsByTenantIdAndName(String tenantId, String name);
}
