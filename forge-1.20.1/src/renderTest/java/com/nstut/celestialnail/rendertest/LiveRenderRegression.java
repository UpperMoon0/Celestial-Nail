package com.nstut.celestialnail.rendertest;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.CataclysmTimeline;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import com.nstut.celestialnail.client.RenderRegressionHooks;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import com.nstut.celestialnail.forge.CelestialNailForge;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Actual integrated-server tracking, production renderer, Oculus, and final-composite RGB. */
public final class LiveRenderRegression implements RenderRegressionHooks.Driver {
    private static final Logger LOG = LogUtils.getLogger();
    private static final long CLOCK = 100000;
    private record Scene(String name, float scale, byte phase, float age, float impactAge,
                         float crumbleAge, double distance, double tipY, String fault) {}
    private record Diff(int changed, long energy, int pixels) {}
    private final Minecraft mc = Minecraft.getInstance();
    private final long started = System.nanoTime();
    private final Path output = mc.gameDirectory.toPath().resolve("render-test-evidence");
    private final List<Map<String, Object>> results = new ArrayList<>();
    private final List<Scene> scenes = List.of(
            new Scene("idle-near", 1, (byte)0, 300, -1, -1, 40, 128, "none"),
            new Scene("idle-close", 1, (byte)0, 300, -1, -1, 3, 128, "none"),
            new Scene("idle-inside", 1, (byte)0, 300, -1, -1, 1, 128, "none"),
            new Scene("idle-underneath", 1, (byte)0, 300, -1, -1, 3, 128, "none"),
            new Scene("embedded-dark", 1, (byte)3, 2300, 2000, -1, 40, -96, "none"),
            new Scene("cloud-overlap", 1, (byte)0, 300, -1, -1, 40, 128, "none"),
            new Scene("idle-far", 1, (byte)0, 300, -1, -1, 150, 128, "none"),
            new Scene("minimum-scale", .1F, (byte)0, 300, -1, -1, 12, 128, "none"),
            new Scene("maximum-scale", 4, (byte)0, 300, -1, -1, 150, 32, "none"),
            new Scene("portal-opening", 1, (byte)0, 15, -1, -1, 90, 32, "none"),
            new Scene("emerging", 1, (byte)0, 100, -1, -1, 90, 128, "none"),
            new Scene("descending", 1, (byte)1, 300, -1, -1, 90, 128, "none"),
            new Scene("impact", 1, (byte)2, 300, 12, -1, 90, 128, "none"),
            new Scene("embedded-buried-anchor", 4, (byte)3, 2300, 2000, -1, 150, -96, "none"),
            new Scene("crumbling", 1, (byte)4, 300, -1, 20, 90, 128, "none"),
            new Scene("terrain-occluded-control", 1, (byte)0, 300, -1, -1, 40, 128, "occluded"),
            new Scene("offscreen-control", 1, (byte)0, 300, -1, -1, 90, 128, "offscreen"),
            new Scene("outside-fade-control", 1, (byte)0, 300, -1, -1, 230, 128, "outside-fade"),
            new Scene("missing-draw-control", 1, (byte)0, 300, -1, -1, 90, 128, "suppress"),
            new Scene("lost-final-composite-control", 1, (byte)0, 300, -1, -1, 90, 128, "erase-composite"),
            new Scene("missing-tracking-control", 1, (byte)0, 300, -1, -1, 90, 128, "untrack"),
            new Scene("cinematic-impact-frame", 1, (byte)2, 300, 0, -1, 90, 128, "none"),
            new Scene("cinematic-sequence-white", 1, (byte)2, 300, 2, -1, 90, 128, "none"),
            new Scene("cinematic-sequence-invert", 1, (byte)2, 300, 4, -1, 90, 128, "none"),
            new Scene("cinematic-sequence-gold", 1, (byte)2, 300, 8, -1, 90, 128, "none"),
            new Scene("cinematic-sequence-ink", 1, (byte)2, 300, 10, -1, 90, 128, "none"),
            new Scene("cinematic-sequence-recovery", 1, (byte)2, 300, 12, -1, 90, 128, "none"),
            new Scene("cinematic-sequence-reduced", 1, (byte)2, 300, 4, -1, 90, 128, "none"),
            new Scene("cinematic-sequence-off-control", 1, (byte)2, 300, 4, -1, 90, 128, "cinematic-off"),
            new Scene("cinematic-delayed-impact-frame", 1, (byte)2, 300, 20, -1, 90, 128, "none"),
            new Scene("cinematic-peripheral-frame", 1, (byte)2, 300, 0, -1, 90, 128, "none"),
            new Scene("cinematic-camera-shake", 1, (byte)2, 300, 0, -1, 90, 128, "none"),
            new Scene("cinematic-dust-below-ledge", 1, (byte)2, 300, 90, -1, 90, 128, "cinematic-ledge"),
            new Scene("cinematic-dust-curtain", 1, (byte)2, 300, 20, -1, 90, 128, "none"),
            new Scene("cinematic-lingering", 1, (byte)3, 2300, 2000, -1, 40, 128, "none"),
            new Scene("cinematic-dust-occluded-control", 1, (byte)2, 300, 20, -1, 90, 128, "cinematic-occluded"),
            new Scene("cinematic-expired-impact-control", 1, (byte)2, 700, 300, -1, 90, 128, "cinematic-control"),
            new Scene("cinematic-disabled-control", 1, (byte)2, 300, 0, -1, 90, 128, "cinematic-disabled")).stream().filter(scene -> {
                String filter=System.getProperty("celestial_nail.renderRegression.filter","");
                return filter.isEmpty() || scene.name.equals(filter);
            }).toList();
    private boolean worldRequested, setupStarted, done, shaders;
    private int index, stage, waited, failures, entityId;
    private String worldName;
    private Scene scene;
    private CompletableFuture<Void> setup;
    private NativeImage hiddenA, hiddenB;
    private long startCinematics;
    private boolean cinematicScene() {return scene.name.startsWith("cinematic-");}
    private double maxCameraYawDelta, maxCameraPitchDelta;
    private boolean sequenceOnly() {return scene.name.startsWith("cinematic-sequence-");}
    private boolean isolatedDust() {return scene.name.equals("cinematic-dust-below-ledge");}
    private boolean delayedImpact() {return scene.name.equals("cinematic-delayed-impact-frame");}
    private long startRenders, startVertices, startApplies, startWrong, startBlend, startColor, startBody, startBodyDepth;
    private float[] hiddenDepth, frameDepth;
    private double cameraY;
    private float pitch, yaw;

    public LiveRenderRegression() {
        if (!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of("run", "render-regression")))
            throw new IllegalStateException("Render tests may only use the isolated run/render-regression directory");
        try { Files.createDirectories(output); } catch (Exception ex) { throw new IllegalStateException(ex); }
        LOG.info("[NailLiveTest] START renderer={} output={}", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER), output);
    }

    @Override public void tick() {
        if (done) return;
        if (System.nanoTime() - started > 12L * 60 * 1_000_000_000L) { abort("Client fixture timed out"); return; }
        try {
            if (!worldRequested && mc.getOverlay() == null) {
                worldRequested = true;
                mc.options.onboardAccessibility = false;
                worldName = "nail-render-fixture-" + UUID.randomUUID();
                LOG.info("[NailLiveTest] Creating disposable world {}", worldName);
                mc.options.pauseOnLostFocus = false;
                mc.options.hideGui = true;
                mc.options.bobView().set(false);
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(60);
                mc.options.fov().set(70);
                mc.options.renderDistance().set(12);
                mc.options.simulationDistance().set(4);
                mc.options.cloudStatus().set(CloudStatus.OFF);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                var rules = new GameRules();
                var settings = new LevelSettings(worldName, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true,
                        rules, WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(worldName, settings, new WorldOptions(8675309, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                                .value().createWorldDimensions());
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            if (mc.screen != null) mc.setScreen(null);
            if (!setupStarted) startScene();
        } catch (Throwable ex) { abort(ex.toString()); }
    }

    private void setShaderMode(boolean enabled) throws Exception {
        Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
        Object config = iris.getMethod("getIrisConfig").invoke(null);
        config.getClass().getMethod("setShaderPackName", String.class).invoke(config, "ComplementaryReimagined_r5.9.3.zip");
        config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, enabled);
        config.getClass().getMethod("save").invoke(config);
        iris.getMethod("reload").invoke(null);
        boolean configured = (boolean) config.getClass().getMethod("areShadersEnabled").invoke(config);
        if (configured != enabled) throw new IllegalStateException("Shader mode switch did not persist");
        if (enabled && !iris.getMethod("getCurrentPackName").invoke(null).toString().contains("Complementary"))
            throw new IllegalStateException("Complementary pack not active");
    }

    private float expectedLaunchAge() {
        if (scene.phase == 0) return -1;
        return 45 + Math.max(0, scene.impactAge) + Math.max(0, scene.crumbleAge);
    }
    private static long timestamp(float age) {
        if (age == -1) return -1;
        if (age < 0 || age >= CLOCK) throw new IllegalArgumentException("Invalid fixture age " + age);
        return CLOCK - (long) age;
    }

    private void startScene() throws Exception {
        if (index >= scenes.size() * 2) { finish(); return; }
        scene = scenes.get(index % scenes.size());
        mc.options.cloudStatus().set(scene.name.equals("cloud-overlap") ? CloudStatus.FANCY : CloudStatus.OFF);
        boolean nextShaders = index >= scenes.size();
        if (index == 0 || shaders != nextShaders) { setShaderMode(nextShaders); shaders = nextShaders; }
        setupStarted = true;
        stage = 0; waited = 0; maxCameraYawDelta = maxCameraPitchDelta = 0;
        RenderRegressionHooks.hideNail = !cinematicScene() || delayedImpact() || isolatedDust() || sequenceOnly();
        RenderRegressionHooks.hideCinematics = true;
        float height = CelestialNailVisuals.height(scene.scale);
        int fixturePower=scene.name.contains("dust")?32:4;
        double target = scene.tipY + height * .5;
        if (scene.name.equals("portal-opening")) target = scene.tipY + height * 1.5;
        if (scene.name.equals("emerging")) target = scene.tipY + (CelestialNailVisuals.portalHeight(height)
                + CelestialNailVisuals.emergenceOffset(CelestialNailVisuals.portalHeight(height), scene.age)) * .5;
        cameraY = target + (scene.name.equals("portal-opening") ? height * .4 : 0);
        if (scene.name.equals("idle-underneath") || scene.name.equals("cloud-overlap")) cameraY = scene.tipY - 60;
        pitch = (float) Math.toDegrees(Math.atan2(cameraY - target, scene.distance));
        yaw = scene.fault.equals("offscreen") || scene.name.equals("cinematic-peripheral-frame") ? 180 : 0;
        if(scene.name.contains("dust")) {
            double origin=scene.tipY+CataclysmTimeline.pierceDepth(fixturePower,height,scene.impactAge);
            cameraY=origin+14;
            pitch=(float)Math.toDegrees(Math.atan2(6,scene.distance));
        }
        if(isolatedDust()) { cameraY-=94; pitch=0; }
        var server = mc.getSingleplayerServer();
        UUID playerId = mc.player.getUUID();
        setup = CompletableFuture.runAsync(() -> {
            var level = server.overworld();
            var player = server.getPlayerList().getPlayer(playerId);
            if (player == null) throw new IllegalStateException("Test player missing");
            var old = new ArrayList<Entity>();
            level.getAllEntities().forEach(entity -> { if (entity instanceof CelestialNailEntity) old.add(entity); });
            old.forEach(Entity::discard);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.setDayTime(scene.name.equals("embedded-dark") ? 18000 : 6000);
            server.getWorldData().overworldData().setGameTime(CLOCK);
            level.setWeatherParameters(0, 100000, false, false);
            // Remove the previous wall, then build a real opaque wall between camera and Nail.
            // Both reference and visible frames contain the wall; Nail pixels must remain absent.
            for (int wallZ : new int[]{-35,-70}) for (int x = -64; x <= 64; x++) for (int y = 128; y <= 200; y++)
                level.setBlock(new net.minecraft.core.BlockPos(x, y, wallZ),
                        ((wallZ==-35 && (scene.fault.equals("occluded") || scene.fault.equals("cinematic-ledge")) || wallZ==-70 && scene.fault.equals("cinematic-occluded")) ? net.minecraft.world.level.block.Blocks.STONE
                                : net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(), 3);
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(level, 0, cameraY - player.getEyeHeight(), -scene.distance, yaw, pitch);
            CelestialNailEntity nail = CelestialNailForge.NAIL.get().create(level);
            if (nail == null) throw new IllegalStateException("Entity factory failed");
            nail.setPos(0, scene.tipY, 0);
            nail.configure("render_fixture_" + index, fixturePower);
            nail.beginSummoning(scene.scale);
            CompoundTag tag = new CompoundTag();
            nail.saveWithoutId(tag);
            // An impact phase without a timestamp is upgraded as an old save.
            // Start before impact so this fixture exercises a genuine transition.
            tag.putByte("Phase", delayedImpact() ? (byte)0 : scene.phase);
            tag.putLong("SummonTime", timestamp(scene.age - (delayedImpact() ? 8 : 0)));
            tag.putLong("LaunchTime", timestamp(expectedLaunchAge() - (delayedImpact() ? 8 : 0)));
            tag.putLong("ImpactTime", delayedImpact() ? -1 : timestamp(scene.impactAge));
            tag.putLong("CrumbleTime", timestamp(scene.crumbleAge));
            tag.putFloat("ImpactYExact", (float) scene.tipY + (scene.impactAge < 0 ? 0
                    : CataclysmTimeline.pierceDepth(fixturePower, height, scene.impactAge)));
            tag.putBoolean("BlastCleared", true);
            nail.load(tag);
            nail.setCustomNameVisible(false);
            level.addFreshEntity(nail);
            entityId = nail.getId();
        }, server);
        LOG.info("[NailLiveTest] SCENE {} shader={} fault={}", scene.name, shaders ? "Complementary" : "off", scene.fault);
    }

    @Override public void beforeFrame() {
        if (done || !setupStarted || setup == null || !setup.isDone() || mc.player == null || mc.level == null) return;
        if (setup.isCompletedExceptionally()) { abort("Server fixture setup failed"); return; }
        mc.level.setGameTime(CLOCK + (delayedImpact() && stage == 2 ? 8 : 0));
        mc.player.setPos(0, cameraY - mc.player.getEyeHeight(), -scene.distance);
        mc.player.xo = mc.player.xOld = mc.player.getX();
        mc.player.yo = mc.player.yOld = mc.player.getY();
        mc.player.zo = mc.player.zOld = mc.player.getZ();
        mc.player.setYRot(yaw); mc.player.yRotO = yaw;
        mc.player.setXRot(pitch); mc.player.xRotO = pitch;
        RenderRegressionHooks.testCameraShake=scene.name.equals("cinematic-camera-shake") && stage==2;
        CelestialNailAtmosphere.shake = 0;
        CelestialNailAtmosphere.flash = 0;
        com.nstut.celestialnail.client.CinematicSettings.impactMode=scene.name.endsWith("sequence-off-control")?0:scene.name.endsWith("sequence-reduced")?1:2;
        com.nstut.celestialnail.client.CinematicSettings.impactIntensity=scene.fault.equals("cinematic-disabled")?0:.8F;
        com.nstut.celestialnail.client.CinematicSettings.dustSamples=scene.fault.equals("cinematic-disabled") || delayedImpact() || sequenceOnly()?0:16;
        com.nstut.celestialnail.client.CinematicSettings.lingering=!scene.fault.equals("cinematic-disabled");
        // Random particles cannot stand in for the actual Nail mesh in an image assertion.
        mc.particleEngine.setLevel(mc.level);
        if (scene.fault.equals("untrack") && stage >= 2) mc.level.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
    }

    @Override public void afterFrame() {
        if (done || !setupStarted || setup == null || !setup.isDone() || mc.player == null || mc.level == null) return;
        try {
            if(RenderRegressionHooks.testCameraShake) {
                var camera=mc.gameRenderer.getMainCamera();
                maxCameraYawDelta=Math.max(maxCameraYawDelta,Math.abs(camera.getYRot()-yaw));
                maxCameraPitchDelta=Math.max(maxCameraPitchDelta,Math.abs(camera.getXRot()-pitch));
            }
            if (stage == 10) {
                // Deliver through real entity metadata, observe it, then simulate
                // eight catch-up game ticks before the first visible render.
                var nail = (CelestialNailEntity)mc.level.getEntity(entityId);
                if (nail == null || nail.impactAge(0) < 0) {
                    if (++waited > 600) throw new IllegalStateException("Delayed impact metadata was not delivered");
                    return;
                }
                CelestialNailAtmosphere.track(nail);
                RenderRegressionHooks.hideCinematics = false;
                stage = 2; waited = 0;
                return;
            }
            // Wait for real spawn/tracking and world render warm-up; absence fails, never skips.
            if (stage == 0 && mc.level.getEntity(entityId) == null && !scene.fault.equals("outside-fade")) {
                if (++waited > 600) throw new IllegalStateException("Server Nail was not tracked by client");
                return;
            }
            if (++waited < (stage == 0 ? 90 : stage == 2 && delayedImpact() ? 1 : 45)) return;
            waited = 0;
            if (stage == 0) {
                hiddenA = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                stage = 1;
            } else if (stage == 1) {
                hiddenB = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                hiddenDepth = scene.name.equals("cloud-overlap") ? frameDepth : null;
                RenderRegressionHooks.hideNail = delayedImpact() || isolatedDust() || sequenceOnly() || !cinematicScene() && scene.fault.equals("suppress");
                RenderRegressionHooks.hideCinematics = delayedImpact() || !cinematicScene();
                startCinematics=com.nstut.celestialnail.client.ProceduralCinematicPass.draws;
                startRenders = RenderRegressionHooks.renders;
                startVertices = RenderRegressionHooks.vertices;
                startApplies = RenderRegressionHooks.applies;
                startWrong = RenderRegressionHooks.wrongPrograms;
                startBlend = RenderRegressionHooks.blendDisabled;
                startColor = RenderRegressionHooks.colorWritesDisabled;
                startBody = RenderRegressionHooks.bodyDraws;
                startBodyDepth = RenderRegressionHooks.bodyDepthDisabled;
                stage = 2;
                if (delayedImpact()) {
                    stage = 10;
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        // Keep receipt age consistent with this fixture's frozen
                        // render clock, including ticks between metadata delivery
                        // and the next render callback.
                        server.getWorldData().overworldData().setGameTime(CLOCK);
                        server.getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(
                                CLOCK, server.overworld().getDayTime(), false));
                        var nail = (CelestialNailEntity)server.overworld().getEntity(entityId);
                        var tag = new CompoundTag();
                        nail.saveWithoutId(tag);
                        tag.putByte("Phase", scene.phase);
                        tag.putLong("ImpactTime", CLOCK - 12);
                        nail.load(tag);
                    });
                }
            } else {
                if (scene.fault.equals("erase-composite")) {
                    // Deliberately lose only the final visible contribution after real draw calls.
                    hiddenB.flipY();
                    int oldTexture = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
                    RenderSystem.bindTexture(mc.getMainRenderTarget().getColorTextureId());
                    hiddenB.upload(0, 0, 0, false);
                    RenderSystem.bindTexture(oldTexture);
                    hiddenB.flipY();
                }
                try (var visible = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { evaluate(visible); }
                hiddenA.close(); hiddenA = null;
                hiddenB.close(); hiddenB = null;
                index++; setupStarted = false;
            }
        } catch (Throwable ex) { abort(ex.toString()); }
    }

    private Diff diff(NativeImage a, NativeImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) throw new IllegalStateException("Window resized during fixture");
        int changed = 0, pixels = 0; long energy = 0;
        for (int y = a.getHeight() / 6; y < a.getHeight() * 5 / 6; y++)
            for (int x = a.getWidth() / 3; x < a.getWidth() * 2 / 3; x++) {
                int rgbA = a.getPixelRGBA(x, y), rgbB = b.getPixelRGBA(x, y), largest = 0;
                for (int channel = 0; channel < 3; channel++) {
                    int delta = Math.abs(((rgbA >> (channel * 8)) & 255) - ((rgbB >> (channel * 8)) & 255));
                    energy += delta; largest = Math.max(largest, delta);
                }
                if (largest > (scene.name.equals("cinematic-lingering") ? 2 : 8)) changed++;
                pixels++;
            }
        return new Diff(changed, energy, pixels);
    }

    private double changedPixelBrightness(NativeImage reference, NativeImage visible) {
        long sum=0, count=0;
        for(int y=visible.getHeight()/6;y<visible.getHeight()*5/6;y++)
            for(int x=visible.getWidth()/3;x<visible.getWidth()*2/3;x++) {
                int a=reference.getPixelRGBA(x,y), b=visible.getPixelRGBA(x,y), delta=0, rgb=0;
                for(int channel=0;channel<3;channel++) {
                    int value=(b>>(channel*8))&255;
                    delta=Math.max(delta,Math.abs(value-((a>>(channel*8))&255)));rgb+=value;
                }
                if(delta>8) {sum+=rgb;count++;}
            }
        return count==0?0:sum/(3.0*count);
    }

    private static boolean changedRgb(int a,int b) {
        for(int c=0;c<3;c++) if(Math.abs(((a>>(8*c))&255)-((b>>(8*c))&255))>8)return true;
        return false;
    }
    private double clippedWhiteFraction(NativeImage reference, NativeImage visible) {
        int changed=0, white=0;
        for(int y=visible.getHeight()/6;y<visible.getHeight()*5/6;y++)
            for(int x=visible.getWidth()/3;x<visible.getWidth()*2/3;x++) {
                int b=visible.getPixelRGBA(x,y);
                if(!changedRgb(reference.getPixelRGBA(x,y),b))continue;
                changed++;
                if((b&255)>=245 && ((b>>8)&255)>=245 && ((b>>16)&255)>=245)white++;
            }
        return changed==0?0:white/(double)changed;
    }
    @Override public void worldRendered() {
        // World depth is cleared for hands/HUD; sample before that clear. Color remains final-frame evidence.
        if (!done && setupStarted && scene.name.equals("cloud-overlap") && stage > 0 && waited == 44)
            frameDepth = captureDepth();
    }
    private float[] captureDepth() {
        var target=mc.getMainRenderTarget();
        int framebuffer=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING);
        int pack=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] keys={org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT,org.lwjgl.opengl.GL11.GL_PACK_ROW_LENGTH,
                org.lwjgl.opengl.GL11.GL_PACK_SKIP_ROWS,org.lwjgl.opengl.GL11.GL_PACK_SKIP_PIXELS};
        int[] old=new int[keys.length];
        var buffer=org.lwjgl.system.MemoryUtil.memAllocFloat(target.width*target.height);
        try {
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER,target.frameBufferId);
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER,0);
            for(int i=0;i<keys.length;i++) {
                old[i]=org.lwjgl.opengl.GL11.glGetInteger(keys[i]);
                org.lwjgl.opengl.GL11.glPixelStorei(keys[i],i==0?1:0);
            }
            org.lwjgl.opengl.GL11.glReadPixels(0,0,target.width,target.height,
                    org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT,org.lwjgl.opengl.GL11.GL_FLOAT,buffer);
            float[] result=new float[buffer.remaining()];buffer.get(result);
            float min=1,max=0;int finite=0;
            for(float depth:result)if(Float.isFinite(depth)){min=Math.min(min,depth);max=Math.max(max,depth);finite++;}
            int error=org.lwjgl.opengl.GL11.glGetError();
            if(error!=0 || finite!=result.length)throw new IllegalStateException("Invalid world depth readback: "+error);
            LOG.info("[NailLiveTest] worldDepth stage={} min={} max={} finite={}",stage,min,max,finite);
            return result;
        } finally {
            for(int i=0;i<keys.length;i++)org.lwjgl.opengl.GL11.glPixelStorei(keys[i],old[i]);
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER,pack);
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER,framebuffer);
            org.lwjgl.system.MemoryUtil.memFree(buffer);
        }
    }
    private int changedDepthPixels(NativeImage reference,NativeImage visible,float[] hidden,float[] drawn) {
        if (hidden == null || drawn == null) throw new IllegalStateException("Missing world depth capture");
        int count=0;
        for(int y=visible.getHeight()/6;y<visible.getHeight()*5/6;y++)
            for(int x=visible.getWidth()/3;x<visible.getWidth()*2/3;x++) {
                int i=(visible.getHeight()-1-y)*visible.getWidth()+x;
                int a=reference.getPixelRGBA(x,y);
                int red=a&255, green=(a>>8)&255, blue=(a>>16)&255;
                // Require clouds behind the visible Nail, not a fixture with only clear sky.
                boolean cloud=Math.min(red,Math.min(green,blue))>=140
                        && Math.max(red,Math.max(green,blue))-Math.min(red,Math.min(green,blue))<(shaders?35:70);
                if(cloud && changedRgb(a,visible.getPixelRGBA(x,y)) && drawn[i]+.00001F<hidden[i])count++;
            }
        return count;
    }
    private void evaluate(NativeImage visible) throws Exception {
        Class<?> apiType = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
        Object api = apiType.getMethod("getInstance").invoke(null);
        boolean activeShaders = (boolean) apiType.getMethod("isShaderPackInUse").invoke(api);
        Diff noise = diff(hiddenA, hiddenB), signal = diff(hiddenB, visible);
        long cinematics=com.nstut.celestialnail.client.ProceduralCinematicPass.draws-startCinematics;
        long renders = RenderRegressionHooks.renders - startRenders;
        long vertices = RenderRegressionHooks.vertices - startVertices;
        long applies = RenderRegressionHooks.applies - startApplies;
        long wrong = RenderRegressionHooks.wrongPrograms - startWrong;
        long blend = RenderRegressionHooks.blendDisabled - startBlend;
        long color = RenderRegressionHooks.colorWritesDisabled - startColor;
        long body = RenderRegressionHooks.bodyDraws - startBody;
        long depthDisabled = RenderRegressionHooks.bodyDepthDisabled - startBodyDepth;
        boolean visiblePixels = signal.changed > Math.max(40, noise.changed * 3)
                && signal.energy > Math.max(2000, noise.energy * 3);
        double brightness=changedPixelBrightness(hiddenB, visible);
        boolean expectedVisible = scene.fault.equals("none") || isolatedDust();
        boolean tracked = mc.level.getEntity(entityId) instanceof CelestialNailEntity;
        List<String> violations = new ArrayList<>();
        if (expectedVisible) {
            if (!tracked) violations.add("MISSING_TRACKING");
            if (!cinematicScene() && renders == 0) violations.add("NO_RENDER");
            if (!cinematicScene() && vertices == 0) violations.add("NO_VERTICES");
            if (!cinematicScene() && applies == 0) violations.add("NO_SHADER_APPLY");
            if (wrong > 0) violations.add("WRONG_PROGRAM");
            if (blend > 0) violations.add("BLENDING_DISABLED");
            if (color > 0) violations.add("COLOR_WRITES_DISABLED");
            if (!visiblePixels) violations.add("FINAL_IMAGE_MISSING");
            if(cinematicScene() && cinematics<=0) violations.add("CINEMATIC_NOT_COMPOSITED");
            if (!cinematicScene() && scene.age >= CelestialNailVisuals.OPEN_TICKS && (body == 0 || depthDisabled > 0)) violations.add("BODY_DEPTH_MISSING");
            if (scene.name.equals("embedded-dark") && brightness < 100) violations.add("DARK_BODY");
        } else {
            if (visiblePixels) violations.add("CONTROL_FALSE_POSITIVE");
            if ((scene.fault.equals("erase-composite") || scene.fault.equals("occluded")) && (renders == 0 || vertices == 0 || applies == 0)) violations.add("CONTROL_NOT_DRAWN");
            if (scene.fault.equals("untrack") && tracked) violations.add("CONTROL_STILL_TRACKED");
        }
        if(scene.fault.equals("cinematic-occluded") && cinematics<=0) violations.add("OCCLUSION_PASS_NOT_EXERCISED");
        if((scene.fault.equals("cinematic-control") || scene.fault.equals("cinematic-disabled") || scene.fault.equals("cinematic-off")) && cinematics>0) violations.add("CINEMATIC_REPLAYED");
        if(scene.name.equals("cinematic-camera-shake") && (maxCameraYawDelta<.1 || maxCameraPitchDelta<.1))
            violations.add("CAMERA_DID_NOT_SHAKE");
        Map<String,Object> lifecycle = new java.util.LinkedHashMap<>();
        if (tracked && mc.level.getEntity(entityId) instanceof CelestialNailEntity nail) {
            lifecycle.put("summonAge", nail.summonAge(0)); lifecycle.put("launchAge", nail.launchAge(0));
            lifecycle.put("impactAge", nail.impactAge(0)); lifecycle.put("crumbleAge", nail.crumbleAge(0));
            lifecycle.put("expectedSummonAge", scene.age); lifecycle.put("expectedLaunchAge", expectedLaunchAge());
            lifecycle.put("expectedImpactAge", scene.impactAge); lifecycle.put("expectedCrumbleAge", scene.crumbleAge);
            if (Math.abs(nail.nailScale()-scene.scale)>.0001 || Math.abs(nail.summonAge(0)-scene.age)>.01
                    || Math.abs(nail.launchAge(0)-expectedLaunchAge())>.01 || Math.abs(nail.impactAge(0)-scene.impactAge)>.01
                    || Math.abs(nail.crumbleAge(0)-scene.crumbleAge)>.01 || nail.isLaunched()!=(scene.phase!=0)
                    || nail.isCrumbling()!=(scene.phase==4) || nail.isImpacting()!=(scene.phase==2 || scene.phase==3))
                violations.add("FIXTURE_STATE_MISMATCH");
        }
        double whiteFraction = clippedWhiteFraction(hiddenB, visible);
        boolean checkSurface = scene.name.equals("idle-near") || scene.name.equals("embedded-dark") || scene.name.equals("cloud-overlap");
        if (checkSurface && whiteFraction > .15) violations.add("BODY_OVEREXPOSED");
        int cloudDepthPixels = scene.name.equals("cloud-overlap") ? changedDepthPixels(hiddenB, visible, hiddenDepth, frameDepth) : -1;
        if (scene.name.equals("cloud-overlap") && cloudDepthPixels < 500) violations.add("CLOUD_BODY_DEPTH_MISSING");
        if (activeShaders != shaders) violations.add("WRONG_SHADER_MODE");
        boolean passed = violations.isEmpty();
        if (!passed) failures++;
        String name = (shaders ? "complementary-" : "vanilla-") + scene.name;
        hiddenB.writeToFile(output.resolve(name + "-hidden.png"));
        visible.writeToFile(output.resolve(name + "-visible.png"));
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("cinematicDraws",cinematics);
        result.put("maxCameraYawDelta",maxCameraYawDelta); result.put("maxCameraPitchDelta",maxCameraPitchDelta);
        result.put("case", name); result.put("passed", passed); result.put("expectedVisible", expectedVisible);
        result.put("shaderPackInUse", activeShaders);
        result.put("lifecycle", lifecycle); result.put("bodyDraws", body); result.put("bodyDepthDisabled", depthDisabled);
        result.put("clippedWhiteFraction", whiteFraction); result.put("cloudDepthPixels", cloudDepthPixels);
        result.put("violations", violations);
        result.put("visiblePixels", visiblePixels); result.put("tracked", tracked); result.put("renders", renders);
        result.put("vertices", vertices); result.put("shaderApplies", applies); result.put("wrongPrograms", wrong);
        result.put("meanVisibleBrightness", brightness); result.put("colorWritesDisabled", color); result.put("blendDisabled", blend); result.put("noise", noise); result.put("signal", signal);
        results.add(result);
        LOG.info("[NailLiveTest] {} {} violations={} tracked={} renders={} vertices={} applies={} wrongPrograms={} blendDisabled={} signal={} noise={}",
                passed ? "PASS" : "FAIL", name, violations, tracked, renders, vertices, applies, wrong, blend, signal, noise);
        writeReport(false);
    }

    private void writeReport(boolean complete) throws Exception {
        var report = Map.of("complete", complete, "failures", failures, "expectedCases", scenes.size() * 2,
                "world", worldName == null ? "not-created" : worldName, "cases", results,
                "renderer", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
        Files.writeString(mc.gameDirectory.toPath().resolve("render-test-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
    private void finish() throws Exception {
        done = true;
        RenderRegressionHooks.hideNail = false;
        RenderRegressionHooks.hideCinematics = false;
        writeReport(true);
        LOG.info("[NailLiveTest] COMPLETE cases={} failures={}", results.size(), failures);
        mc.stop();
    }
    private void abort(String reason) {
        done = true; failures++;
        LOG.error("[NailLiveTest] ABORT {}", reason);
        try { writeReport(false); } catch (Exception ex) { LOG.error("Cannot write live test report", ex); }
        mc.stop();
    }
}
