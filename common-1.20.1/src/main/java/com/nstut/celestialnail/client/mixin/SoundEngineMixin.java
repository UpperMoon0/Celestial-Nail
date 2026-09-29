package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
@Mixin(net.minecraft.client.sounds.SoundEngine.class)
public abstract class SoundEngineMixin {
 @Inject(method="calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",at=@At("RETURN"),cancellable=true)
 private void celestial$muffle(SoundInstance sound,CallbackInfoReturnable<Float> cir) {
  if(!sound.getLocation().getNamespace().equals("celestial_nail") && sound.getSource()!=SoundSource.MASTER && sound.getSource()!=SoundSource.VOICE)
   cir.setReturnValue(cir.getReturnValue()*(1-CelestialNailAtmosphere.muffle));
 }
}
