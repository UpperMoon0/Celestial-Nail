package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public abstract class GameRendererMixin {
 @Inject(method="getDepthFar",at=@At("RETURN"),cancellable=true)
 private void celestial$far(CallbackInfoReturnable<Float> cir) {cir.setReturnValue(CelestialNailAtmosphere.farPlane(cir.getReturnValue()));}
 @Inject(method="reloadShaders",at=@At("TAIL"))
 private void celestial$shader(net.minecraft.server.packs.resources.ResourceProvider provider,CallbackInfo ci) throws java.io.IOException {
  com.nstut.celestialnail.client.CelestialNailSkyRender.reload(provider);
 }
}
