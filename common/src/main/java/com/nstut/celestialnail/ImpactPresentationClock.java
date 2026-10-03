package com.nstut.celestialnail;

/** One-shot receipt grace for an already tracked Nail. Joining an old impact never arms it. */
public final class ImpactPresentationClock {
    private boolean sawBeforeImpact, observedImpact;
    private boolean pending;
    private long firstRenderNanos = Long.MIN_VALUE;
    public void observe(float serverAge) {
        if (serverAge < 0) { sawBeforeImpact = true; return; }
        if (observedImpact) return;
        observedImpact = true;
        // A delayed metadata packet should not swallow the brief pulse. Do not replay stale events.
        pending = sawBeforeImpact && serverAge < 40;
    }
    public float age(long renderNanos, float serverAge) {
        if (serverAge < 0 || !pending) return serverAge;
        // Tick catch-up and terrain work must not consume the pulse before a frame
        // is submitted. Start at the first render, then use monotonic real time.
        if (firstRenderNanos == Long.MIN_VALUE) {
            if (serverAge >= 40) { pending = false; return serverAge; }
            firstRenderNanos = renderNanos;
        }
        return Math.max(0, (renderNanos - firstRenderNanos) / 50_000_000F);
    }
}
