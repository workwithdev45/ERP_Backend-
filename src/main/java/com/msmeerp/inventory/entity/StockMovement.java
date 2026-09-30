package com.msmeerp.inventory.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** W8: the append-only stock ledger — every quantity change is one row here, never edited or deleted. */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Only meaningful on a stock-in movement — feeds the item's weighted-average cost. */
    @Column(name = "unit_cost", precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "total_value", precision = 16, scale = 4)
    private BigDecimal totalValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 30)
    private AdjustmentReason reasonCode;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "performed_at", nullable = false)
    @Builder.Default
    private Instant performedAt = Instant.now();

    public enum MovementType {
        IN, OUT, ADJUSTMENT, TRANSFER, RETURN, OPENING
    }

    /** Reason codes for manual adjustments, distinct from the freeform {@link #reason} note. */
    public enum AdjustmentReason {
        DAMAGE, LOST, FOUND, RECOUNT, EXPIRED, OTHER
    }
}
