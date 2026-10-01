package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.StockMovement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // JOIN FETCH product/warehouse — both are LAZY, and open-in-view is off, so mapping to a DTO
    // after the repository call returns would otherwise hit a closed-session LazyInitializationException.
    @Query("SELECT m FROM StockMovement m JOIN FETCH m.product JOIN FETCH m.warehouse " +
            "WHERE m.tenantId = :tenantId ORDER BY m.performedAt DESC, m.id DESC")
    List<StockMovement> findRecent(@Param("tenantId") String tenantId, Pageable limit);

    @Query("SELECT m FROM StockMovement m JOIN FETCH m.product JOIN FETCH m.warehouse " +
            "WHERE m.tenantId = :tenantId AND m.product.id = :productId ORDER BY m.performedAt DESC")
    List<StockMovement> findByTenantIdAndProductIdOrderByPerformedAtDesc(
            @Param("tenantId") String tenantId, @Param("productId") Long productId);
}
