package com.hms.hospital.service;

import com.hms.tenant.entity.Tenant;

public interface HospitalProvisioningService {

    Tenant provisionHospital(String adminEmail, String portalId, boolean startBlank);
}
