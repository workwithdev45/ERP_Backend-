package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
    List<StockBatch> findByTenantId(String tenantId);
    List<StockBatch> findByTenantIdAndStockItemId(String tenantId, Long stockItemId);
    List<StockBatch> findByTenantIdAndExpiryDateIsNotNullAndExpiryDateBetween(
            String tenantId, LocalDate from, LocalDate to);
}
