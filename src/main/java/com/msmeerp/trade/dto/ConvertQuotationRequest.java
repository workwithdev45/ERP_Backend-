package com.msmeerp.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvertQuotationRequest {
    @NotNull(message = "Choose the warehouse to reserve stock in")
    private Long warehouseId;
    private LocalDate dueDate;
    private String partyReference;
}
