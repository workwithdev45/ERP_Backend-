package com.msmeerp.auth.security;

import com.msmeerp.common.util.AppConstants;
import com.msmeerp.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromJwt(jwt);
                String tokenTenantId = tokenProvider.getTenantIdFromJwt(jwt);
                String resolvedTenantId = TenantContext.getTenantId();

                // The subdomain (or X-Tenant-ID header) was already resolved by TenantFilter.
                // If a JWT shows up claiming a *different* tenant than the one this request
                // actually arrived on, reject it outright rather than silently trusting the
                // token — a leaked/stolen JWT for tenant A must not work against tenant B's
                // subdomain just because it's a valid signature.
                if (StringUtils.hasText(tokenTenantId)
                        && StringUtils.hasText(resolvedTenantId)
                        && !AppConstants.DEFAULT_TENANT_ID.equalsIgnoreCase(resolvedTenantId)
                        && !tokenTenantId.equals(resolvedTenantId)) {
                    log.warn("Rejecting request: JWT tenant {} does not match resolved tenant {}", tokenTenantId, resolvedTenantId);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token does not belong to this tenant");
                    return;
                }

                if (StringUtils.hasText(tokenTenantId)) {
                    TenantContext.setTenantId(tokenTenantId);
                }

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AppConstants.BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
