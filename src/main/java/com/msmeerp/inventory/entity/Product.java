package com.msmeerp.inventory.entity;

import com.msmeerp.tenant.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends TenantAwareEntity {

    @NotBlank
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @NotBlank
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    @Builder.Default
    private ItemType itemType = ItemType.STOCK;

    /** GST HSN/SAC code — required for a compliant tax invoice, captured here so Sales doesn't ask again. */
    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    /** Standard GST slab (0/5/12/18/28) applied to this item's line amount. */
    @Column(name = "gst_rate_percent", precision = 5, scale = 2)
    private BigDecimal gstRatePercent;

    @Column(name = "barcode", length = 100)
    private String barcode;

    /** Just a URL for now — no image upload/storage pipeline exists yet. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "uom", length = 50)
    private String unitOfMeasure;

    /** e.g. "Box" when {@link #conversionFactor} pieces of {@link #unitOfMeasure} make one. Both null/empty means no alternate unit. */
    @Column(name = "secondary_unit", length = 50)
    private String secondaryUnit;

    @Column(name = "conversion_factor", precision = 12, scale = 4)
    private BigDecimal conversionFactor;

    @NotNull
    @Column(name = "reorder_level", nullable = false)
    @Builder.Default
    private Integer reorderLevel = 0;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
}
