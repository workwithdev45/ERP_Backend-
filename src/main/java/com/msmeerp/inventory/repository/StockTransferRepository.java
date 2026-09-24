package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    Page<StockTransfer> findByTenantId(String tenantId, Pageable pageable);
}
