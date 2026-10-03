package com.nstut.celestialnail.client.mixin;

import com.nstut.celestialnail.client.CelestialNailCinematics;
import com.nstut.celestialnail.client.ProceduralCinematicPass;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // Keep the actual projection object: vanilla subsequently applies hurt/bob/nausea to it.
    @Unique private Matrix4f celestial$projection;
    @ModifyVariable(method="renderLevel",at=@At("STORE"),ordinal=0)
    private Matrix4f celestial$projection(Matrix4f projection) {
        celestial$projection=projection;
        return projection;
    }
    @Inject(method="renderLevel",at=@At(value="INVOKE",
            target="Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",ordinal=2))
    private void celestial$capture(DeltaTracker delta,CallbackInfo ci) {
        CelestialNailCinematics.capture(delta.getGameTimeDeltaPartialTick(false),celestial$projection);
    }
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void celestial$composite(CallbackInfo ci) { CelestialNailCinematics.render(); }
    @Inject(method="close",at=@At("HEAD"))
    private void celestial$close(CallbackInfo ci) { ProceduralCinematicPass.close(); }
}
