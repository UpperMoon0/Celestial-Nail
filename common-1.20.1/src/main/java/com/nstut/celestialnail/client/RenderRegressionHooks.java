package com.nstut.celestialnail.client;

/** Dormant bridge to the isolated renderTest source set; no test driver is shipped. */
public final class RenderRegressionHooks {
    public static final boolean ACTIVE = Boolean.getBoolean("celestial_nail.renderRegression");
    public interface Driver {
        void tick();
        void beforeFrame();
        void afterFrame();
    }
    private static Driver driver;
    public static boolean hideNail;
    public static long renders, vertices, applies, wrongPrograms, blendDisabled, colorWritesDisabled;
    private RenderRegressionHooks() {}

    public static boolean fixture(String id) { return ACTIVE && id.startsWith("render_fixture_"); }
    public static void tick() {
        if (!ACTIVE) return;
        if (driver == null) {
            try {
                driver = (Driver) Class.forName("com.nstut.celestialnail.rendertest.LiveRenderRegression")
                        .getConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Render regression requested without its test driver", ex);
            }
        }
        driver.tick();
    }
    public static void beforeFrame() { if (driver != null) driver.beforeFrame(); }
    public static void afterFrame() { if (driver != null) driver.afterFrame(); }
    public static void shaderApplied(net.minecraft.client.renderer.ShaderInstance shader) {
        if (!ACTIVE) return;
        applies++;
        if (org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM) != shader.getId()) wrongPrograms++;
        if (!org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND)) blendDisabled++;
        int[] mask = colorWriteMask();
        if (mask[0] == 0 || mask[1] == 0 || mask[2] == 0) colorWritesDisabled++;
    }
    public static int[] colorWriteMask() {
        int[] mask = new int[4];
        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_COLOR_WRITEMASK, mask);
        return mask;
    }
}
