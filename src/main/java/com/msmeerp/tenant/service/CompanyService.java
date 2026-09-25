package com.msmeerp.tenant.service;

import com.msmeerp.tenant.dto.CompanyDetailsResponse;
import com.msmeerp.tenant.dto.CompanyDetailsUpdateRequest;

/** G3: reading and editing the current tenant's own company/GST details (Settings -> Company). */
public interface CompanyService {
    CompanyDetailsResponse getCompanyDetails();
    CompanyDetailsResponse updateCompanyDetails(CompanyDetailsUpdateRequest request);
}
