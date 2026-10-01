package com.nstut.celestialnail;

import com.nstut.celestialnail.client.RenderDebugRateLimiter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderDebugRateLimiterTest {
    @Test void boundsRepeatedEventsAndDoesNotBurstAfterAStall() {
        var gate = new RenderDebugRateLimiter();
        assertTrue(gate.acquire(0, 100));
        for (long time = 0; time < 100; time++) assertFalse(gate.acquire(time, 100));
        assertTrue(gate.acquire(100, 100));
        assertTrue(gate.acquire(10000, 100));
        assertFalse(gate.acquire(10000, 100));
        assertFalse(gate.acquire(10099, 100));
        assertTrue(gate.acquire(10100, 100));
    }

    @Test void toleratesNanoTimeWrapAndChangedInterval() {
        var gate = new RenderDebugRateLimiter();
        assertTrue(gate.acquire(Long.MAX_VALUE - 20, 100));
        assertFalse(gate.acquire(Long.MIN_VALUE + 20, 100));
        assertTrue(gate.acquire(Long.MIN_VALUE + 80, 100));
        assertFalse(gate.acquire(Long.MIN_VALUE + 85, 10));
        assertTrue(gate.acquire(Long.MIN_VALUE + 90, 10));
        assertThrows(IllegalArgumentException.class, () -> gate.acquire(0, 0));
    }
}
