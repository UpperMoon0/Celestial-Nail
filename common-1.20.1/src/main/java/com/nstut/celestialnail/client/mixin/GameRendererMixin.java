package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public abstract class GameRendererMixin {
 @Inject(method="renderLevel",at=@At(value="INVOKE",shift=At.Shift.AFTER,
  target="Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"))
 private void celestial$testWorldDepth(CallbackInfo ci) {com.nstut.celestialnail.client.RenderRegressionHooks.worldRendered();}
 @ModifyVariable(method="render",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private float celestial$stableTestPartialTick(float partialTick) {
  return com.nstut.celestialnail.client.RenderRegressionHooks.ACTIVE ? 0 : partialTick;
 }
 @Inject(method="render",at=@At("HEAD"))
 private void celestial$testBeforeFrame(CallbackInfo ci) {com.nstut.celestialnail.client.RenderRegressionHooks.beforeFrame();}
 @Inject(method="render",at=@At("RETURN"))
 private void celestial$testAfterFrame(CallbackInfo ci) {com.nstut.celestialnail.client.RenderRegressionHooks.afterFrame();}
 @Inject(method="getDepthFar",at=@At("RETURN"),cancellable=true)
 private void celestial$far(CallbackInfoReturnable<Float> cir) {cir.setReturnValue(CelestialNailAtmosphere.farPlane(cir.getReturnValue()));}
}
