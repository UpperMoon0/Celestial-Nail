package com.nstut.celestialnail.client;

/** Monotonic, constant-space gate shared by all render diagnostics. */
public final class RenderDebugRateLimiter {
    private boolean started;
    private long last;

    public boolean acquire(long now, long intervalNanos) {
        if (intervalNanos <= 0) throw new IllegalArgumentException("interval must be positive");
        if (started && now - last < intervalNanos) return false;
        started = true;
        last = now;
        return true;
    }
}
