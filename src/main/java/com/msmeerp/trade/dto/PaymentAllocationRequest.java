package com.msmeerp.trade.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class PaymentAllocationRequest {
    @NotNull
    private Long documentId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Allocated amount must be positive")
    private BigDecimal amount;
}
