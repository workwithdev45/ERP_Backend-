package com.hms.user.repository;

import com.hms.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    List<User> findByTenantIdAndBranchId(String tenantId, String branchId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.tenantId = :tenantId AND u.branch.id = :branchId AND r.name = :roleName AND u.status = :status")
    Optional<User> findAdminByTenantIdAndBranchId(
            @Param("tenantId") String tenantId,
            @Param("branchId") String branchId,
            @Param("roleName") String roleName,
            @Param("status") User.UserStatus status
    );

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE u.tenantId = :tenantId AND u.branch.id = :branchId AND r.name = :roleName AND u.status = :status")
    boolean existsAdminByTenantIdAndBranchId(
            @Param("tenantId") String tenantId,
            @Param("branchId") String branchId,
            @Param("roleName") String roleName,
            @Param("status") User.UserStatus status
    );
}
