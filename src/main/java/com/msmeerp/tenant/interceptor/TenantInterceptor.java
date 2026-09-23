package com.msmeerp.tenant.interceptor;

import com.msmeerp.tenant.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Processing request URI [{}] for Tenant [{}]", request.getRequestURI(), tenantId);
        // Can optionally set session variables in datasource connection if using RLS
        return true;
    }
}
