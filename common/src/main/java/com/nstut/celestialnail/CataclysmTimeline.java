package com.nstut.celestialnail;

/** Shared, bounded audiovisual timing. Distances are blocks, time is ticks. */
public final class CataclysmTimeline {
    public static final int AFTERMATH_TICKS=1200;
    private CataclysmTimeline() {}
    public static float horizontalFade(double distance, double range) {
        return 1-CelestialNailVisuals.smooth((float)((distance-Math.max(0,range-32))/Math.min(32,range)));
    }
    public static float shockRadius(float age) { return Math.max(0,age-8)*12; }
    public static float arrival(double distance) { return 8+(float)distance/12; }
    public static float aftermath(float age) { return 1-CelestialNailVisuals.smooth(age/AFTERMATH_TICKS); }
    public static float shake(float sinceArrival) {
        return sinceArrival<0 ? 0 : (float)Math.exp(-sinceArrival/18)*Math.min(1,sinceArrival/2);
    }
    public static boolean shockHits(double distance,float age,float interval) {
        return age>=8 && age<=70 && distance<=shockRadius(age) && distance>=shockRadius(Math.max(8,age-interval));
    }
    public static float pierceDepth(float power,float height,float age) {
        float t=Math.max(0,Math.min(1,age/18));
        return (power+height*.18F)*(1-(1-t)*(1-t)*(1-t));
    }
    public static double nextDescentSpeed(double speed) { return Math.min(18,Math.max(2.5,speed)+.65); }
    public static float charge(float launchAge) {
        return launchAge<0 ? 0 : CelestialNailVisuals.smooth(launchAge/24);
    }
}
