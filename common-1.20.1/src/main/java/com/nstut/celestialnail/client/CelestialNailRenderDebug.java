package com.nstut.celestialnail.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Render-thread only. No per-entity maps, per-frame logs, or GL queries per vertex. */
public final class CelestialNailRenderDebug {
    private static final Logger LOG = LogUtils.getLogger();
    private static final RenderDebugRateLimiter CONFIG = new RenderDebugRateLimiter();
    private static final RenderDebugRateLimiter REPORT = new RenderDebugRateLimiter();
    private static final RenderDebugRateLimiter ERRORS = new RenderDebugRateLimiter();
    private static boolean enabled, forceVisible, disableDistanceFade, pixelProbe, requireVisibleDraw, shaderCaptured, renderCaptured, programMismatch;
    private static int changedPixels = -1;
    private static long interval = 5_000_000_000L;
    private static long frames, checks, distanceRejected, frustumRejected, renders, vertices, shaderApplies, reloads;
    private static float minAlpha = Float.POSITIVE_INFINITY, maxAlpha = Float.NEGATIVE_INFINITY;
    private static String lastRender = "none", shaderState = "not observed", reloadState = "not observed";

    private CelestialNailRenderDebug() {}
    public static boolean enabled() { return enabled; }
    public static boolean forceVisible() { return enabled && forceVisible; }
    public static boolean disableDistanceFade() { return enabled && disableDistanceFade; }

    public static void tick() {
        long now = System.nanoTime();
        if (CONFIG.acquire(now, 2_000_000_000L)) loadConfig(now);
        if (!enabled || !REPORT.acquire(now, interval)) return;
        Minecraft mc = Minecraft.getInstance();
        int nails = 0, readyNails = 0;
        StringBuilder tracked = new StringBuilder();
        if (mc.level != null) for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof CelestialNailEntity nail)) continue;
            nails++;
            if (nail.summonAge(0) >= com.nstut.celestialnail.CelestialNailVisuals.READY_TICKS) readyNails++;
            if (nails <= 4) tracked.append(" [entity=").append(nail.getId()).append(" nail=").append(nail.nailId())
                    .append(" pos=").append(nail.position()).append(" bounds=").append(nail.visualBounds()).append(']');
        }
        var result = RenderDebugChecks.evaluate(renders, vertices, maxAlpha, shaderApplies, programMismatch, changedPixels);
        String check = !requireVisibleDraw || readyNails == 0 || frames == 0 ? "NOT_ARMED"
                : result == RenderDebugChecks.Result.PIXELS_CHANGED ? "PASS"
                : result == RenderDebugChecks.Result.DRAW_OBSERVED ? "UNVERIFIED" : "FAIL";
        LOG.info("[NailRenderDebug] check={} result={} changedPixels={} pixelProbe={} dimension={} camera={} renderDistance={} trackedNails={}{} frames={} checks={} distanceRejected={} frustumRejected={} renders={} vertices={} alpha=[{},{}] shaderApplies={} reloads={} forceVisible={} disableDistanceFade={} customShader={} oculus={} lastRender={} shaderDraw={} reload={}",
                check, result, changedPixels, pixelProbe,
                mc.level == null ? "none" : mc.level.dimension().location(), mc.gameRenderer.getMainCamera().getPosition(),
                mc.options.getEffectiveRenderDistance(), nails, tracked, frames, checks, distanceRejected, frustumRejected,
                renders, vertices, vertices == 0 ? "n/a" : minAlpha, vertices == 0 ? "n/a" : maxAlpha,
                shaderApplies, reloads, forceVisible, disableDistanceFade, CelestialNailSkyRender.debugShader(),
                oculusState(), lastRender, shaderState, reloadState);
        reset();
    }

    private static void loadConfig(long now) {
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/celestial_nail-render-debug.properties");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, "# Client render diagnostics; reloads every 2 seconds.\n"
                        + "enabled=false\nlogIntervalSeconds=5\nforceVisible=false\ndisableDistanceFade=false\n"
                        + "pixelProbe=false\nrequireVisibleDraw=false\n");
            }
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(path)) { properties.load(reader); }
            boolean nextEnabled = Boolean.parseBoolean(properties.getProperty("enabled", "false"));
            long seconds = Long.parseLong(properties.getProperty("logIntervalSeconds", "5").trim());
            interval = Math.max(1, Math.min(300, seconds)) * 1_000_000_000L;
            forceVisible = Boolean.parseBoolean(properties.getProperty("forceVisible", "false"));
            disableDistanceFade = Boolean.parseBoolean(properties.getProperty("disableDistanceFade", "false"));
            pixelProbe = Boolean.parseBoolean(properties.getProperty("pixelProbe", "false"));
            requireVisibleDraw = Boolean.parseBoolean(properties.getProperty("requireVisibleDraw", "false"));
            if (enabled != nextEnabled) reset();
            enabled = nextEnabled;
        } catch (IOException | IllegalArgumentException ex) {
            if (ERRORS.acquire(now, 30_000_000_000L)) LOG.warn("[NailRenderDebug] Cannot load {}; retaining previous settings: {}", path, ex.toString());
        }
    }

    public static void frame() { if (enabled) frames++; }
    public static void visibility(boolean distance, boolean frustum) {
        if (!enabled) return;
        checks++;
        if (!distance) distanceRejected++;
        if (!frustum) frustumRejected++;
    }
    public static void render(CelestialNailEntity nail, float age, float visibility, float portalOffset, int packedLight) {
        if (!enabled) return;
        renders++;
        if (renderCaptured) return;
        renderCaptured = true;
        lastRender = "entity=" + nail.getId() + " nail=" + nail.nailId() + " age=" + age + " height=" + nail.nailHeight()
                + " portalOffset=" + portalOffset + " visibility=" + visibility + " light=" + packedLight
                + " launched=" + nail.isLaunched() + " impacting=" + nail.isImpacting();
    }
    public static void vertex(float alpha) {
        if (!enabled) return;
        vertices++;
        minAlpha = Math.min(minAlpha, alpha);
        maxAlpha = Math.max(maxAlpha, alpha);
    }
    public static void shaderApplied(ShaderInstance shader) {
        if (!enabled) return;
        shaderApplies++;
        if (shaderCaptured) return;
        shaderCaptured = true;
        int activeProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        programMismatch = activeProgram != shader.getId();
        shaderState = "name=" + shader.getName() + " expectedProgram=" + shader.getId()
                + " glProgram=" + activeProgram
                + " drawFramebuffer=" + GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                + " mainFramebuffer=" + Minecraft.getInstance().getMainRenderTarget().frameBufferId
                + " depthTest=" + GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                + " depthWrite=" + GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
                + " colorWriteMask=" + java.util.Arrays.toString(RenderRegressionHooks.colorWriteMask())
                + " blend=" + GL11.glIsEnabled(GL11.GL_BLEND) + " cull=" + GL11.glIsEnabled(GL11.GL_CULL_FACE)
                + " colorModulator=" + java.util.Arrays.toString(RenderSystem.getShaderColor());
        if (pixelProbe) NailDrawPixelProbe.begin();
    }
    public static void shaderClearing(ShaderInstance shader) {
        if (enabled && pixelProbe) {
            int result = NailDrawPixelProbe.finish();
            if (result >= 0) changedPixels = result;
        }
    }
    public static void reloaded(String outcome) {
        if (!enabled) return;
        reloads++;
        reloadState = outcome;
    }
    public static boolean isEnabled() { return enabled; }
    private static String oculusState() {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = iris.getMethod("getInstance").invoke(null);
            return "packInUse=" + iris.getMethod("isShaderPackInUse").invoke(api)
                    + " shadowPass=" + iris.getMethod("isRenderingShadowPass").invoke(api);
        } catch (ClassNotFoundException ex) { return "absent";
        } catch (ReflectiveOperationException | LinkageError ex) { return "unavailable:" + ex.getClass().getSimpleName(); }
    }
    private static void reset() {
        frames = checks = distanceRejected = frustumRejected = renders = vertices = shaderApplies = reloads = 0;
        minAlpha = Float.POSITIVE_INFINITY;
        maxAlpha = Float.NEGATIVE_INFINITY;
        shaderCaptured = renderCaptured = false;
        programMismatch = false;
        changedPixels = -1;
        NailDrawPixelProbe.cancel();
        lastRender = "none";
        shaderState = reloadState = "not observed";
    }
}
