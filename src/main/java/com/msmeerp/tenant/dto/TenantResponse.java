package com.msmeerp.tenant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    private String id;
    @JsonAlias({"tenantName", "name"})
    private String name;
    private String tenantName;
    private String subdomain;
    private String portalId;
    private boolean active;
    private Map<String, String> settings;
}
