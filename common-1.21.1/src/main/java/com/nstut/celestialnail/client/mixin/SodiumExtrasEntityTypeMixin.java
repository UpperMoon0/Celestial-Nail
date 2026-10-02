package com.nstut.celestialnail.client.mixin;

import com.nstut.celestialnail.CelestialNail;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Let the Nail renderer cull its complete visual bounds instead of its anchor. */
@Mixin(value = EntityType.class, priority = 900)
public abstract class SodiumExtrasEntityTypeMixin {
    @Dynamic("Added to EntityType by Sodium Extras 1.0.x")
    @Inject(method = "embPlus$isAllowed", at = @At("HEAD"), cancellable = true,
            remap = false, require = 0)
    private void celestialNail$exemptAnchorDistanceCulling(CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>)(Object)this);
        if (id != null && CelestialNail.MOD_ID.equals(id.getNamespace())
                && "celestial_nail".equals(id.getPath())) {
            cir.setReturnValue(true);
        }
    }
}
