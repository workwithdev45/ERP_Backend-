package com.msmeerp.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** G3: the legal/GST details needed to produce a correct invoice, shown on Settings -> Company. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailsResponse {
    private String portalId;
    private String name;
    private String legalName;
    private String gstin;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private int financialYearStartMonth;
    private String businessType;
}
