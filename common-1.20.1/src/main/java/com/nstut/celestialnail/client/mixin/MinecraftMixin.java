package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MinecraftMixin {
 @Inject(method="tick",at=@At("TAIL")) private void celestial$tick(CallbackInfo ci) {CelestialNailAtmosphere.tick();com.nstut.celestialnail.compat.SodiumExtrasTestHooks.tick();com.nstut.celestialnail.client.CelestialNailRenderDebug.tick();com.nstut.celestialnail.client.RenderRegressionHooks.tick();}
}
