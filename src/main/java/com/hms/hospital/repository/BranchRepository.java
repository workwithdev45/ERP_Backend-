package com.hms.hospital.repository;

import com.hms.hospital.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findByTenantId(String tenantId);
    Optional<Branch> findByTenantIdAndId(String tenantId, String id);
    
    @Query("SELECT b FROM Branch b WHERE b.tenantId = :tenantId AND (LOWER(b.branchName) = LOWER(:name) OR b.branchName = :name)")
    Optional<Branch> findByTenantIdAndName(@Param("tenantId") String tenantId, @Param("name") String name);

    Optional<Branch> findByTenantIdAndBranchName(String tenantId, String branchName);
    boolean existsByTenantIdAndBranchName(String tenantId, String branchName);
}
