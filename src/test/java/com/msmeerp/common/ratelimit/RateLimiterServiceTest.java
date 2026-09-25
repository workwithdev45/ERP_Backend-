package com.msmeerp.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiter = new RateLimiterService();

    @Test
    void allowsUpToTheConfiguredMaximumWithinTheWindow() {
        String key = "otp-send:user@example.com";

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.allow(key, 5, Duration.ofMinutes(15))).as("attempt %d", i + 1).isTrue();
        }
    }

    @Test
    void blocksOnceTheMaximumIsExceeded() {
        String key = "otp-send:blocked@example.com";
        for (int i = 0; i < 5; i++) {
            rateLimiter.allow(key, 5, Duration.ofMinutes(15));
        }

        assertThat(rateLimiter.allow(key, 5, Duration.ofMinutes(15))).isFalse();
    }

    @Test
    void tracksSeparateKeysIndependently() {
        String keyA = "login:a@example.com";
        String keyB = "login:b@example.com";

        for (int i = 0; i < 5; i++) {
            rateLimiter.allow(keyA, 5, Duration.ofMinutes(15));
        }

        assertThat(rateLimiter.allow(keyA, 5, Duration.ofMinutes(15))).isFalse();
        assertThat(rateLimiter.allow(keyB, 5, Duration.ofMinutes(15))).isTrue();
    }

    @Test
    void resetClearsTheCountForThatKey() {
        String key = "login:reset@example.com";
        for (int i = 0; i < 5; i++) {
            rateLimiter.allow(key, 5, Duration.ofMinutes(15));
        }
        assertThat(rateLimiter.allow(key, 5, Duration.ofMinutes(15))).isFalse();

        rateLimiter.reset(key);

        assertThat(rateLimiter.allow(key, 5, Duration.ofMinutes(15))).isTrue();
    }

    @Test
    void allowsAgainOnceTheWindowExpires() throws InterruptedException {
        String key = "login:window-expiry@example.com";
        Duration tinyWindow = Duration.ofMillis(50);

        assertThat(rateLimiter.allow(key, 1, tinyWindow)).isTrue();
        assertThat(rateLimiter.allow(key, 1, tinyWindow)).isFalse();

        Thread.sleep(80);

        assertThat(rateLimiter.allow(key, 1, tinyWindow)).isTrue();
    }
}
