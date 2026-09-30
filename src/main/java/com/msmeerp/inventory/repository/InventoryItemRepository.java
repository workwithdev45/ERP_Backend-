package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByTenantIdAndProductIdAndWarehouseId(String tenantId, Long productId, Long warehouseId);

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product p JOIN FETCH i.warehouse w WHERE i.tenantId = :tenantId")
    List<InventoryItem> findAllWithDetails(@Param("tenantId") String tenantId);

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product p JOIN FETCH i.warehouse w " +
            "WHERE i.tenantId = :tenantId AND (p.reorderLevel >= i.availableQuantity OR i.availableQuantity <= 0)")
    List<InventoryItem> findLowStockItems(@Param("tenantId") String tenantId);

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product p JOIN FETCH i.warehouse w " +
            "WHERE i.tenantId = :tenantId AND i.warehouse.id = :warehouseId")
    List<InventoryItem> findByWarehouseId(@Param("tenantId") String tenantId, @Param("warehouseId") Long warehouseId);

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product p JOIN FETCH i.warehouse w " +
            "WHERE i.tenantId = :tenantId AND i.product.id = :productId")
    List<InventoryItem> findByProductId(@Param("tenantId") String tenantId, @Param("productId") Long productId);
}
