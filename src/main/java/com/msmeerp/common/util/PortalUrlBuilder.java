package com.msmeerp.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The one place every emailed link is built (G7) — subdomain-per-workspace in production,
 * or {@code FRONTEND_URL} in local development where wildcard subdomains don't resolve.
 */
@Component
public class PortalUrlBuilder {

    @Value("${app.portal.base-domain:msmeerp.com}")
    private String baseDomain;

    @Value("${app.portal.scheme:https}")
    private String scheme;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    /** The workspace's origin, e.g. {@code https://acme.msmeerp.com} or {@code http://localhost:5173} in dev. */
    public String originFor(String portalId) {
        if (StringUtils.hasText(frontendUrl)) {
            return frontendUrl.replaceAll("/+$", "");
        }
        return "%s://%s.%s".formatted(scheme, portalId, baseDomain);
    }
}
