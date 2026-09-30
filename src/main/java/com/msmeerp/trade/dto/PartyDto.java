package com.msmeerp.trade.dto;

import com.msmeerp.trade.entity.PartyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyDto {
    private Long id;
    private PartyType partyType;
    private String name;
    private String gstin;
    private String phone;
    private String email;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private Integer paymentTermsDays;
    private BigDecimal creditLimit;
    private Boolean active;
    /** Unpaid invoices (customers) or bills (vendors). */
    private BigDecimal outstanding;
}
