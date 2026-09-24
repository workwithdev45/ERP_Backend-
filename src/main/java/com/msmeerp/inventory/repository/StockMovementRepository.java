package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByTenantId(String tenantId);

    Page<StockMovement> findByTenantIdAndStockItemIdAndWarehouseId(
            String tenantId, Long stockItemId, Long warehouseId, Pageable pageable);

    Page<StockMovement> findByTenantIdAndStockItemId(String tenantId, Long stockItemId, Pageable pageable);

    Page<StockMovement> findByTenantIdAndWarehouseId(String tenantId, Long warehouseId, Pageable pageable);

    Page<StockMovement> findByTenantId(String tenantId, Pageable pageable);

    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m " +
            "where m.tenantId = :tenantId and m.stockItem.id = :stockItemId")
    BigDecimal sumQuantityByStockItem(@Param("tenantId") String tenantId, @Param("stockItemId") Long stockItemId);

    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m " +
            "where m.tenantId = :tenantId and m.stockItem.id = :stockItemId and m.warehouse.id = :warehouseId")
    BigDecimal sumQuantityByStockItemAndWarehouse(
            @Param("tenantId") String tenantId, @Param("stockItemId") Long stockItemId, @Param("warehouseId") Long warehouseId);

    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m " +
            "where m.tenantId = :tenantId and m.stockBatch.id = :stockBatchId")
    BigDecimal sumQuantityByStockBatch(@Param("tenantId") String tenantId, @Param("stockBatchId") Long stockBatchId);
}
