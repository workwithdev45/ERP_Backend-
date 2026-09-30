package com.msmeerp.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One payment applied to one bill/invoice — shown from either side. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationDto {
    private Long paymentId;
    private String paymentNumber;
    private LocalDate paymentDate;
    private String mode;
    private Long documentId;
    private String documentNumber;
    private BigDecimal amount;
}
