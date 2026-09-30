package com.msmeerp.compliance.repository;

import com.msmeerp.compliance.entity.EInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EInvoiceRepository extends JpaRepository<EInvoice, Long> {
    Optional<EInvoice> findByTenantIdAndDocumentId(String tenantId, Long documentId);

    boolean existsByIrn(String irn);
}
