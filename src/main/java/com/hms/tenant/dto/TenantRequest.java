package com.hms.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRequest {

    @NotBlank(message = "Tenant ID is required")
    @Size(min = 3, max = 50, message = "Tenant ID must be between 3 and 50 characters")
    private String id;

    @NotBlank(message = "Hospital/Tenant Name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Subdomain is required")
    @Size(min = 3, max = 50)
    private String subdomain;
}
