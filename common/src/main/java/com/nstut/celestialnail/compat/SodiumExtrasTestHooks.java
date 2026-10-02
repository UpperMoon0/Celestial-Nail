package com.nstut.celestialnail.compat;

/** Dormant test bridge. The isolated driver is never included in release artifacts. */
public final class SodiumExtrasTestHooks {
    public static final boolean ACTIVE = Boolean.getBoolean("celestial_nail.packagedCompatTest");
    public interface Driver {
        void tick();
        void beforeFrame();
        void afterFrame();
    }
    private static Driver driver;
    public static boolean hideNail;
    public static long renders;
    private SodiumExtrasTestHooks() {}
    public static void tick() {
        if (!ACTIVE) return;
        if (driver == null) {
            try {
                driver = (Driver) Class.forName("com.nstut.celestialnail.compattest.SodiumExtrasClientFixture")
                        .getConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Compatibility test requested without its isolated driver", ex);
            }
        }
        driver.tick();
    }
    public static void beforeFrame() { if (driver != null) driver.beforeFrame(); }
    public static void afterFrame() { if (driver != null) driver.afterFrame(); }
    public static boolean hide(String id) {
        if (!ACTIVE || !id.startsWith("compat_fixture_")) return false;
        if (hideNail) return true;
        renders++;
        return false;
    }
}
