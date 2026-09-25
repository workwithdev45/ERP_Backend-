package com.msmeerp.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/** G7: every emailed link must be built the same way, in prod and in local dev. */
class PortalUrlBuilderTest {

    private final PortalUrlBuilder builder = new PortalUrlBuilder();

    @Test
    void buildsASubdomainUrlWhenNoFrontendUrlOverrideIsConfigured() {
        ReflectionTestUtils.setField(builder, "baseDomain", "msmeerp.com");
        ReflectionTestUtils.setField(builder, "scheme", "https");
        ReflectionTestUtils.setField(builder, "frontendUrl", "");

        assertThat(builder.originFor("acme-traders")).isEqualTo("https://acme-traders.msmeerp.com");
    }

    @Test
    void prefersTheFrontendUrlOverrideWhenConfigured() {
        ReflectionTestUtils.setField(builder, "baseDomain", "msmeerp.com");
        ReflectionTestUtils.setField(builder, "scheme", "https");
        ReflectionTestUtils.setField(builder, "frontendUrl", "http://localhost:5173");

        assertThat(builder.originFor("acme-traders")).isEqualTo("http://localhost:5173");
    }

    @Test
    void stripsATrailingSlashFromTheOverride() {
        ReflectionTestUtils.setField(builder, "baseDomain", "msmeerp.com");
        ReflectionTestUtils.setField(builder, "scheme", "https");
        ReflectionTestUtils.setField(builder, "frontendUrl", "http://localhost:5173/");

        assertThat(builder.originFor("acme-traders")).isEqualTo("http://localhost:5173");
    }
}
