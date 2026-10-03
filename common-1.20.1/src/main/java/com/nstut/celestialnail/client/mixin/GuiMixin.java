package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiMixin {
 @Inject(method="render",at=@At("HEAD")) private void celestial$overlay(net.minecraft.client.gui.GuiGraphics g,float partial,CallbackInfo ci) {
  int w=g.guiWidth(),h=g.guiHeight();
  int dark=(int)(CelestialNailAtmosphere.darkness*255);
  if(dark>0)g.fill(0,0,w,h,(dark<<24)|0x07132e);
 }
}
