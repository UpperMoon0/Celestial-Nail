package com.nstut.celestialnail;

/** Server-clock animation, so tracking/reconnecting clients see the same summon sequence. */
public final class CelestialNailVisuals {
    public static final float DEFAULT_HEIGHT = 72;
    public static final float MIN_SCALE = .1F, MAX_SCALE = 4;
    public static final int PORTAL_LAYOUT_VERSION = 2;
    public static final int OPEN_TICKS = 30, EMERGE_TICKS = 140, CLOSE_TICKS = 30;
    public static final int READY_TICKS = OPEN_TICKS + EMERGE_TICKS;
    private CelestialNailVisuals() {}
    public static float safeScale(float scale) {
        return Float.isFinite(scale) ? Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale)) : 1;
    }
    public static float smooth(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t*t*(3-2*t);
    }
    public static float opening(float age, float launchAge) {
        return smooth(age / OPEN_TICKS) * (launchAge < 0 ? 1 : 1-smooth(launchAge / CLOSE_TICKS));
    }
    public static float emergence(float age) { return smooth((age-OPEN_TICKS)/EMERGE_TICKS); }
    public static float height(float scale) { return DEFAULT_HEIGHT * safeScale(scale); }
    /** Half a nail-height of clear sky between the floating crown and the aperture. */
    public static float portalHeight(float nailHeight) { return nailHeight * 1.5F; }
    public static float emergenceOffset(float portalOffset, float age) {
        return portalOffset * (1-emergence(age));
    }
    /** Conservative camera-space depth bound for the entire tall body, not just its crown. */
    public static double bodyFarPlane(double horizontalDistance, double cameraY, double tipY, double height) {
        double vertical=Math.max(Math.abs(tipY-cameraY),Math.abs(tipY+height-cameraY));
        return Math.hypot(horizontalDistance+height*.65,vertical)+128;
    }

}
