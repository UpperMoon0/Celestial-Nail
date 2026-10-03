package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public abstract class GameRendererMixin {
 // After world post-processing, before the first hand/HUD depth clear and GUI draw.
 @Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V",ordinal=0,remap=false))
 private void celestial$compositeCinematic(CallbackInfo ci) {com.nstut.celestialnail.client.CelestialNailCinematics.render();}
 @Inject(method="close",at=@At("HEAD"))
 private void celestial$closeCinematic(CallbackInfo ci) {com.nstut.celestialnail.client.ProceduralCinematicPass.close();}

 @Inject(method="renderLevel",at=@At(value="INVOKE",shift=At.Shift.AFTER,
  target="Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
 private void celestial$captureCinematic(net.minecraft.client.DeltaTracker delta,CallbackInfo ci) {
  com.nstut.celestialnail.client.CelestialNailCinematics.capture(delta.getGameTimeDeltaPartialTick(false));
 }

 @Inject(method="render",at=@At("HEAD"))
 private void celestial$testBeforeFrame(CallbackInfo ci) {com.nstut.celestialnail.compat.SodiumExtrasTestHooks.beforeFrame();}
 @Inject(method="render",at=@At("RETURN"))
 private void celestial$testAfterFrame(CallbackInfo ci) {com.nstut.celestialnail.compat.SodiumExtrasTestHooks.afterFrame();}
 @Inject(method="getDepthFar",at=@At("RETURN"),cancellable=true)
 private void celestial$far(CallbackInfoReturnable<Float> cir) {cir.setReturnValue(CelestialNailAtmosphere.farPlane(cir.getReturnValue()));}
}
