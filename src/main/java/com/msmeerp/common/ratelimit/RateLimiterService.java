package com.msmeerp.common.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fixed-window rate limiter, in-process (a single Portainer/EC2 instance runs one backend
 * container, so this is sufficient for now). If the app ever scales to multiple instances,
 * swap the backing map for Redis (INCR + EXPIRE) without changing the call sites below —
 * {@link #allow(String, int, Duration)} is the only method callers use.
 */
@Service
public class RateLimiterService {

    private record Window(AtomicReference<Instant> resetAt, java.util.concurrent.atomic.AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return true if the caller is within {@code maxAttempts} for this {@code key} within the
     * current {@code window}; false if the limit has been exceeded (caller should reject the request).
     */
    public boolean allow(String key, int maxAttempts, Duration window) {
        Instant now = Instant.now();
        Window w = windows.computeIfAbsent(key, k -> new Window(
                new AtomicReference<>(now.plus(window)),
                new java.util.concurrent.atomic.AtomicInteger(0)));

        synchronized (w) {
            if (now.isAfter(w.resetAt().get())) {
                w.resetAt().set(now.plus(window));
                w.count().set(0);
            }
            return w.count().incrementAndGet() <= maxAttempts;
        }
    }

    /** Clears a key's window early, e.g. after a successful login resets the failure counter. */
    public void reset(String key) {
        windows.remove(key);
    }
}
