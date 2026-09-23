package com.msmeerp.tenant.service.impl;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.tenant.dto.TenantRequest;
import com.msmeerp.tenant.dto.TenantResponse;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.tenant.service.TenantResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantResolverServiceImpl implements TenantResolverService {

    private final TenantRepository tenantRepository;

    @Override
    @Cacheable(value = "tenant_validity", key = "#tenantId", unless = "#result == false")
    public boolean isValidTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return tenantRepository.existsByIdAndActiveTrue(tenantId);
    }

    @Override
    @Cacheable(value = "tenant_name", key = "#tenantId", unless = "#result == null")
    public String getTenantName(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return tenantRepository.findById(tenantId)
                .map(Tenant::getName)
                .orElse(null);
    }

    @Override
    @Transactional
    public TenantResponse createTenant(TenantRequest request) {
        if (tenantRepository.existsById(request.getId())) {
            throw new BadRequestException("Tenant ID already exists: " + request.getId());
        }

        Tenant tenant = Tenant.builder()
                .id(request.getId())
                .name(request.getName())
                .subdomain(request.getSubdomain())
                .active(true)
                .build();

        Tenant saved = tenantRepository.save(tenant);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(String tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
        return mapToResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .tenantName(tenant.getName())
                .subdomain(tenant.getSubdomain())
                .portalId(tenant.getPortalId())
                .active(tenant.isActive())
                .settings(new HashMap<>())
                .build();
    }
}
