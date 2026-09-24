package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.StockItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Page<StockItem> findByTenantId(String tenantId, Pageable pageable);
    List<StockItem> findByTenantId(String tenantId);
    Optional<StockItem> findByTenantIdAndSku(String tenantId, String sku);
    boolean existsByTenantIdAndSku(String tenantId, String sku);
}
