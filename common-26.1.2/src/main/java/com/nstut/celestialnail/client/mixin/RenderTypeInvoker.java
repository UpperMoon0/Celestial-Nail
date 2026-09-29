package com.nstut.celestialnail.client.mixin;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(net.minecraft.client.renderer.rendertype.RenderType.class)
public interface RenderTypeInvoker {
 @Invoker("create") static net.minecraft.client.renderer.rendertype.RenderType celestial$create(String name,net.minecraft.client.renderer.rendertype.RenderSetup setup) {throw new AssertionError();}
}
