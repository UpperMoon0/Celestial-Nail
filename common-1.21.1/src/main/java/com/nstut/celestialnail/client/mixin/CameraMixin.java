package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.Camera.class)
public abstract class CameraMixin {
 @Shadow protected abstract void setRotation(float yaw,float pitch);
 @Shadow public abstract float getXRot();
 @Shadow public abstract float getYRot();
 @Inject(method="setup",at=@At("TAIL")) private void celestial$shake(CallbackInfo ci) {
  float s=CelestialNailAtmosphere.shake; double t=System.nanoTime()*1.0e-8;
  if(s>0.001F)setRotation(getYRot()+(float)Math.sin(t*1.7)*s*.8F,getXRot()+(float)Math.sin(t*2.3)*s*.6F);
 }
 
}
