package com.nstut.celestialnail.rendertest;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import com.nstut.celestialnail.forge.CelestialNailForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** Real transformed EntityType and dispatcher calls; no world or terrain mutation. */
@Mod.EventBusSubscriber(modid = "celestial_nail_render_test", value = Dist.CLIENT)
public final class SodiumExtrasCompatRegression {
    private static boolean done;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("celestial_nail.sodiumExtrasCompatTest") || Boolean.getBoolean("celestial_nail.packagedCompatTest") || done
                || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        done = true;
        Map<String, Object> results = new LinkedHashMap<>();
        try {
            boolean present;
            java.lang.reflect.Method allowed = null;
            try {
                Class<?> api = Class.forName("toni.sodiumextras.foundation.entitydistance.IWhitelistCheck");
                allowed = api.getMethod("embPlus$isAllowed");
                present = true;
            } catch (ClassNotFoundException absent) { present = false; }
            results.put("extrasPresent", present);
            results.put("expectedExtrasProperty", System.getProperty("celestial_nail.expectSodiumExtras"));
            require(present == Boolean.getBoolean("celestial_nail.expectSodiumExtras"), "optional mod presence");
            var type = CelestialNailForge.NAIL.get();
            if (present) {
                require(Boolean.TRUE.equals(allowed.invoke(type)), "Nail exemption on first lookup");
                require(Boolean.TRUE.equals(allowed.invoke(type)), "Nail exemption on cached lookup");
                require(Boolean.FALSE.equals(allowed.invoke(EntityType.ITEM)), "ordinary entity remains subject to culling");
                require(Boolean.TRUE.equals(allowed.invoke(EntityType.GHAST)), "user whitelist remains respected");
            }
            // Constructors accept a null level; these entities are never ticked or added to a world.
            var nail = new CelestialNailEntity(type, null);
            nail.setPos(85, 200, 1393);
            // Isolate Extras' distance decision from vanilla's much shorter item range.
            var item = new ItemEntity(EntityType.ITEM, null) {
                @Override public boolean shouldRender(double x, double y, double z) { return true; }
            };
            item.setPos(85, 200, 1393);
            Frustum visible = frustum(true), hidden = frustum(false);
            var dispatcher = mc.getEntityRenderDispatcher();
            require(dispatcher.shouldRender(nail, visible, 50, 115, 1393), "Nail survives vertical anchor cutoff");
            require(dispatcher.shouldRender(nail, visible, -65, 180, 1393), "Nail survives horizontal anchor cutoff");
            require(!dispatcher.shouldRender(nail, hidden, 50, 180, 1393), "Nail still respects frustum");
            require(!dispatcher.shouldRender(nail, visible, -2000, 180, 1393), "Nail still respects its own distance limit");
            if (present) {
                require(dispatcher.getRenderer(item).shouldRender(item, visible, 50, 115, 1393), "ordinary control passes renderer visibility");
                require(!dispatcher.shouldRender(item, visible, 50, 115, 1393), "ordinary entity vertical cutoff");
                require(!dispatcher.shouldRender(item, visible, -65, 180, 1393), "ordinary entity horizontal cutoff");
                var extrasConfig = Class.forName("toni.sodiumextras.EmbyConfig");
                Object whitelist = extrasConfig.getField("entityWhitelist").get(null);
                Object entries = whitelist.getClass().getMethod("get").invoke(whitelist);
                require(!entries.toString().contains("celestial_nail"), "compatibility does not change user config");
            }
            results.put("passed", true);
        } catch (Throwable failure) {
            results.put("passed", false);
            results.put("failure", failure.toString());
            LogUtils.getLogger().error("[NailSodiumExtrasTest] Failed", failure);
        }
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("sodium-extras-compat-results.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(results));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
        LogUtils.getLogger().info("[NailSodiumExtrasTest] {}", results);
        mc.stop();
    }

    private static Frustum frustum(boolean visible) {
        return new Frustum(new Matrix4f(), new Matrix4f()) {
            @Override public boolean isVisible(AABB bounds) { return visible; }
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
