package com.msmeerp.trade.dto;

import com.msmeerp.trade.entity.PartyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyRequest {

    @NotNull(message = "Choose customer, vendor or both")
    private PartyType partyType;

    @NotBlank(message = "Name is required")
    private String name;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "GSTIN must be 15 characters, e.g. 27AAPFU0939F1ZV")
    private String gstin;

    private String phone;
    private String email;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;

    @Min(value = 0, message = "Payment terms can't be negative")
    private Integer paymentTermsDays;

    @DecimalMin(value = "0", message = "Credit limit can't be negative")
    private BigDecimal creditLimit;

    private Boolean active;
}
