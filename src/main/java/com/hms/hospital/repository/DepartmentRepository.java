package com.hms.hospital.repository;

import com.hms.hospital.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByTenantIdAndBranchId(String tenantId, String branchId);
    List<Department> findByTenantId(String tenantId);
    Optional<Department> findByTenantIdAndBranchIdAndCode(String tenantId, String branchId, String code);
}
