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
    Optional<InventoryItem> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product p JOIN FETCH i.warehouse w")
    List<InventoryItem> findAllWithDetails();

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product p JOIN FETCH i.warehouse w WHERE p.reorderLevel >= i.availableQuantity OR i.availableQuantity <= 0")
    List<InventoryItem> findLowStockItems();

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product p JOIN FETCH i.warehouse w WHERE i.warehouse.id = :warehouseId")
    List<InventoryItem> findByWarehouseId(@Param("warehouseId") Long warehouseId);
}
