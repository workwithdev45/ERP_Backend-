package com.msmeerp.inventory.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItem extends TenantAwareEntity {

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "uom", nullable = false, length = 20)
    private String uom;

    @Column(name = "reorder_threshold", nullable = false, precision = 15, scale = 2)
    private BigDecimal reorderThreshold;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;
}
