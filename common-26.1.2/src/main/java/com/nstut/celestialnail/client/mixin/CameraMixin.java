package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.Camera.class)
public abstract class CameraMixin {
 @Shadow protected abstract void setRotation(float yaw,float pitch);
 @Shadow public abstract float xRot();
 @Shadow public abstract float yRot();
 @Inject(method="alignWithEntity",at=@At("TAIL")) private void celestial$shake(CallbackInfo ci) {
  float s=CelestialNailAtmosphere.cameraShake(); double t=System.nanoTime()*1.0e-9;
  if(s>0.001F)setRotation(yRot()+(float)(Math.sin(t*43)+.3*Math.sin(t*71))*s*1.6F,xRot()+(float)(Math.sin(t*53)+.25*Math.sin(t*83))*s*1.1F);
 }
 @Shadow private float depthFar;
 @Inject(method="update",at=@At(value="FIELD",target="Lnet/minecraft/client/Camera;depthFar:F",opcode=181,shift=At.Shift.AFTER))
 private void celestial$far(CallbackInfo ci) {depthFar=CelestialNailAtmosphere.farPlane(depthFar);}
}
