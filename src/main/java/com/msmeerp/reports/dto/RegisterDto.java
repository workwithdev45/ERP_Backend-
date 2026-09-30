package com.msmeerp.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDto {
    private LocalDate from;
    private LocalDate to;
    private List<RegisterRowDto> rows;
    /** Net of notes, same sign convention as the rows. */
    private RegisterRowDto totals;
    private List<GstRateSummaryDto> byGstRate;
}
