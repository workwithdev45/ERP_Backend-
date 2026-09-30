package com.msmeerp.compliance.repository;

import com.msmeerp.compliance.entity.EwayBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EwayBillRepository extends JpaRepository<EwayBill, Long> {
    List<EwayBill> findByTenantIdAndDocumentIdOrderByIdDesc(String tenantId, Long documentId);

    Optional<EwayBill> findByTenantIdAndId(String tenantId, Long id);
}
