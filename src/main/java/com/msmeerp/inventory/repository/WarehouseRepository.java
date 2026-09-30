package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByTenantIdAndCode(String tenantId, String code);

    Optional<Warehouse> findByTenantIdAndId(String tenantId, Long id);

    List<Warehouse> findByTenantIdOrderByNameAsc(String tenantId);

    List<Warehouse> findByTenantIdAndDefaultWarehouseTrue(String tenantId);
}
