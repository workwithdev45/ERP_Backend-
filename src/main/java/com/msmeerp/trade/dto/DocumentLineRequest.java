package com.msmeerp.trade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One line of a new document. For a document created from another (GRN from PO, invoice from
 * challan, ...) only {@link #sourceLineId} and {@link #quantity} are needed — the rest is copied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentLineRequest {
    private Long productId;
    private Long sourceLineId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0", message = "Rate can't be negative")
    private BigDecimal rate;

    @DecimalMin(value = "0", message = "Discount can't be negative")
    @DecimalMax(value = "100", message = "Discount can't exceed 100%")
    private BigDecimal discountPercent;

    @DecimalMin(value = "0", message = "GST rate can't be negative")
    @DecimalMax(value = "100", message = "GST rate can't exceed 100%")
    private BigDecimal gstRate;
}
