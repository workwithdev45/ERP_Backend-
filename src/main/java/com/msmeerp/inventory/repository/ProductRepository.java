package com.msmeerp.inventory.repository;

import com.msmeerp.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByTenantIdAndSku(String tenantId, String sku);

    Optional<Product> findByTenantIdAndId(String tenantId, Long id);

    Optional<Product> findByTenantIdAndBarcode(String tenantId, String barcode);

    List<Product> findByTenantIdOrderByNameAsc(String tenantId);
}
