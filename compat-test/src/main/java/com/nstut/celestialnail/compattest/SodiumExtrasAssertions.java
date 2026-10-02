package com.nstut.celestialnail.compattest;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.util.Map;
/** Loader-independent assertions against the actually transformed dispatcher and EntityType. */
public final class SodiumExtrasAssertions {
 private static final java.util.List<String> checks=new java.util.ArrayList<>();
 @SuppressWarnings("unchecked")
 public static EntityType<CelestialNailEntity> nailType() {
  return (EntityType<CelestialNailEntity>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse("celestial_nail:celestial_nail"));
 }
 public static void run(Minecraft mc, Map<String,Object> results) throws Exception {
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
            var type = nailType();
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
                require(entries.equals(java.util.List.of("minecraft:ghast")), "compatibility preserves exact user whitelist");
                for (var setting : java.util.Map.of("entityDistanceCulling", true, "entityCullingDistanceX", 4096, "entityCullingDistanceY", 32).entrySet()) {
                    Object value = extrasConfig.getField(setting.getKey()).get(null);
                    require(setting.getValue().equals(value.getClass().getMethod("get").invoke(value)), "configured " + setting.getKey());
                }
            }
 results.put("cullingAssertions",java.util.List.copyOf(checks));
 }
 private static Frustum frustum(boolean visible) {
  return new Frustum(new Matrix4f(),new Matrix4f()) {
   @Override public boolean isVisible(AABB bounds) { return visible; }
  };
 }
 public static void require(boolean condition,String message) {
  if(!condition) throw new AssertionError(message);
  checks.add(message);
 }
}
