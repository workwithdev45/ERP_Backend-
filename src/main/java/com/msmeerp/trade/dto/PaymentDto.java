package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    private Long id;
    private String paymentNumber;
    private String direction;
    private Long partyId;
    private String partyName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;
    private String mode;
    private String reference;
    private String notes;
    private List<AllocationDto> allocations;
}
