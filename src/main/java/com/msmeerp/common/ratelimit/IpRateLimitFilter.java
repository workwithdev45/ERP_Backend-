package com.msmeerp.common.ratelimit;

import com.msmeerp.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Coarse per-IP backstop on top of the per-email limits applied in each service (G5). This catches
 * an attacker who rotates target emails from a single machine; the per-email limits catch the
 * reverse (many machines targeting one victim email).
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class IpRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, int[]> LIMITS = Map.of(
            // path prefix -> {maxAttempts, windowMinutes}
            "/api/v1/auth/login", new int[]{20, 15},
            "/api/v1/auth/forgot-password", new int[]{10, 60},
            "/api/v1/companies/register", new int[]{10, 60},
            "/api/v1/companies/verify-otp", new int[]{20, 15},
            "/api/v1/companies/find", new int[]{10, 60}
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int[] limit = LIMITS.get(path);

        if (limit != null) {
            String ip = clientIp(request);
            boolean allowed = rateLimiterService.allow("ip:" + path + ":" + ip, limit[0], Duration.ofMinutes(limit[1]));
            if (!allowed) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResponse.error("Too many requests. Please wait a while and try again.")));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
