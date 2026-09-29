package com.nstut.celestialnail.client.mixin;

import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A tall nail remains visible when its buried anchor's terrain section is culled. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Unique private boolean celestialNail$renderingNail;
    @Shadow public abstract boolean isChunkCompiled(BlockPos pos);

    @Redirect(method="renderLevel",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos celestialNail$captureEntity(Entity entity) {
        // This runs for each entity admitted by the normal distance/frustum checks.
        celestialNail$renderingNail=entity instanceof CelestialNailEntity;
        return entity.blockPosition();
    }

    @Redirect(method="renderLevel",at=@At(value="INVOKE",
            target="Lnet/minecraft/client/renderer/LevelRenderer;isChunkCompiled(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean celestialNail$visibleAboveBuriedAnchor(LevelRenderer renderer, BlockPos pos) {
        // Keep ordinary entities' terrain check. Nail geometry still depth-tests against terrain,
        // uses its own frustum bounds, and fades at the configured horizontal chunk distance.
        return celestialNail$renderingNail || isChunkCompiled(pos);
    }
}
