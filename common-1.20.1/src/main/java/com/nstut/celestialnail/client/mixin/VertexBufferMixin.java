package com.nstut.celestialnail.client.mixin;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.nstut.celestialnail.client.CelestialNailRenderDebug;
import com.nstut.celestialnail.client.RenderRegressionHooks;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin {
    // Observe after every ShaderInstance.apply injection has finished, including Oculus's color lock.
    @Inject(method = "_drawWithShader", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V"))
    private void celestialNail$beforeActualDraw(Matrix4f modelView, Matrix4f projection,
                                               ShaderInstance shader, CallbackInfo ci) {
        if (!com.nstut.celestialnail.client.NailDrawContext.mainNailDraw()) return;
        CelestialNailRenderDebug.shaderApplied(shader);
        RenderRegressionHooks.shaderApplied(shader);
    }
    @Inject(method = "_drawWithShader", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V"))
    private void celestialNail$afterActualDraw(Matrix4f modelView, Matrix4f projection,
                                              ShaderInstance shader, CallbackInfo ci) {
        if (com.nstut.celestialnail.client.NailDrawContext.mainNailDraw())
            CelestialNailRenderDebug.shaderClearing(shader);
    }
}
