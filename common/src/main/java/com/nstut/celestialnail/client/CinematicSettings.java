package com.nstut.celestialnail.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Local accessibility/performance preferences. Malformed values fall back to restrained defaults. */
public final class CinematicSettings {
    public static int impactMode = com.nstut.celestialnail.ImpactSequence.FULL;
    public static float impactIntensity = .8F;
    public static float shakeIntensity = 1;
    public static int dustSamples = 16;
    public static boolean lingering = true;
    private static boolean loaded;
    private CinematicSettings() {}
    public static void load(Path gameDirectory) {
        if (loaded) return;
        loaded = true;
        Path path = gameDirectory.resolve("config/celestial-nail-cinematics.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(path)) {
                try (var in = Files.newBufferedReader(path)) { p.load(in); }
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, "# Client-only; restart the client after editing. Intensities: 0..1.\n"
                        + "# Impact mode: full, reduced (no flashing), off. Full contains strong flashes.\nimpactMode=full\nimpactIntensity=0.8\nshakeIntensity=1.0\n"
                        + "# Dust ray-march samples: 0 disables dust; otherwise 8..24.\n"
                        + "dustSamples=16\nlingeringAtmosphere=true\n");
            }
            impactMode = com.nstut.celestialnail.ImpactSequence.mode(p.getProperty("impactMode","full").trim());
            impactIntensity = intensity(p.getProperty("impactIntensity"), .8F);
            shakeIntensity = intensity(p.getProperty("shakeIntensity"), 1);
            int samples = samples(p.getProperty("dustSamples", "16"));
            dustSamples = samples <= 0 ? 0 : Math.max(8, Math.min(24, samples));
            lingering = !"false".equalsIgnoreCase(p.getProperty("lingeringAtmosphere", "true").trim());
        } catch (IOException | IllegalArgumentException ex) {
            System.getLogger("CelestialNail").log(System.Logger.Level.WARNING,
                    "Could not read cinematic settings; using defaults", ex);
        }
    }
    public static int samples(String value) {
        try {
            int n=Integer.parseInt(value);
            return n<=0?0:Math.max(8,Math.min(24,n));
        } catch (IllegalArgumentException ex) {return 16;}
    }
    public static float intensity(String value, float fallback) {
        try {
            float f = Float.parseFloat(value);
            return Float.isFinite(f) ? Math.max(0, Math.min(1, f)) : fallback;
        } catch (IllegalArgumentException | NullPointerException ex) { return fallback; }
    }
}
