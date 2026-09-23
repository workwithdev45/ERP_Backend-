package com.msmeerp.tenant.repository;

import com.msmeerp.tenant.entity.UserTenantMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTenantMapRepository extends JpaRepository<UserTenantMap, Long> {

    Optional<UserTenantMap> findByEmail(String email);

    boolean existsByEmail(String email);
}
