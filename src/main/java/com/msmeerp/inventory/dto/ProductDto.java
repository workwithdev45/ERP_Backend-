package com.msmeerp.inventory.dto;

import com.msmeerp.inventory.entity.ItemType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    private Long id;

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;
    private String category;

    @Builder.Default
    private ItemType itemType = ItemType.STOCK;

    private String hsnCode;

    @DecimalMin(value = "0", message = "GST rate can't be negative")
    @DecimalMax(value = "100", message = "GST rate can't exceed 100%")
    private BigDecimal gstRatePercent;

    private String barcode;
    private String imageUrl;
    private String unitOfMeasure;
    private String secondaryUnit;

    @DecimalMin(value = "0", inclusive = false, message = "Conversion factor must be positive")
    private BigDecimal conversionFactor;

    @NotNull(message = "Reorder level is required")
    private Integer reorderLevel;

    private Boolean active;

    /** Only used when creating a new product: seeds an opening-stock movement in this warehouse. */
    private Long openingStockWarehouseId;
    private Integer openingStockQuantity;
    private BigDecimal openingStockUnitCost;
}
