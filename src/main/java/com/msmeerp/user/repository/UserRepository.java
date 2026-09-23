package com.msmeerp.user.repository;

import com.msmeerp.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTenantIdAndUsername(String tenantId, String username);
    Optional<User> findByTenantIdAndEmail(String tenantId, String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Page<User> findByTenantId(String tenantId, Pageable pageable);
    boolean existsByTenantIdAndUsername(String tenantId, String username);
    boolean existsByTenantIdAndEmail(String tenantId, String email);
}
