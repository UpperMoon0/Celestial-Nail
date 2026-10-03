package com.nstut.celestialnail;

/** Shared envelopes: synchronized world ages for dust, rendered presentation age for the pulse. */
public final class CinematicTimeline {
    private CinematicTimeline() {}
    public static float impactFrame(float age) {
        return ImpactSequence.strength(age,ImpactSequence.FULL);
    }
    /** Render-time impulse followed by the traveling shockwave, bounded to one degree scale. */
    public static float cameraShake(float age, double distance) {
        if (age < 0 || age >= 100 || !Double.isFinite(distance)) return 0;
        float contact = (float)Math.exp(-age / 16);
        float wave = CataclysmTimeline.shake(age - CataclysmTimeline.arrival(distance)) * .55F;
        return Math.max(contact, wave);
    }
    public static float dust(float age) {
        if (age < 0 || age >= 240) return 0;
        return CelestialNailVisuals.smooth(age / 6) * (1 - CelestialNailVisuals.smooth((age - 90) / 150));
    }
    public static float proximity(double distance, double range) {
        if (!Double.isFinite(distance) || !Double.isFinite(range) || range <= 0) return 0;
        return 1 - CelestialNailVisuals.smooth((float)(Math.max(0, distance) / range));
    }
    public static float lingering(double distance, float age) {
        return age < CataclysmTimeline.AFTERMATH_TICKS ? 0 : proximity(distance, 96) * .12F;
    }
}
