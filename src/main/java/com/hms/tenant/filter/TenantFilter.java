package com.hms.tenant.filter;

import com.hms.common.util.AppConstants;
import com.hms.tenant.context.TenantContext;
import com.hms.tenant.entity.Tenant;
import com.hms.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantHeader = request.getHeader(AppConstants.TENANT_HEADER);
            String tenantId = tenantHeader;
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = AppConstants.DEFAULT_TENANT_ID;
            } else if (!tenantId.equalsIgnoreCase(AppConstants.DEFAULT_TENANT_ID)) {
                Optional<Tenant> t = tenantRepository.findByPortalId(tenantId);
                if (t.isPresent()) {
                    tenantId = t.get().getId();
                }
            }
            TenantContext.setTenantId(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
