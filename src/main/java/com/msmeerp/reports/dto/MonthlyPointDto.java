package com.msmeerp.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyPointDto {
    /** yyyy-MM */
    private String month;
    private BigDecimal sales;
    private BigDecimal purchases;
}
