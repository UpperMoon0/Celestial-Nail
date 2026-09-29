package com.nstut.celestialnail.mixin;

import com.nstut.celestialnail.NailMutationScope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class NailBlockCallbacksMixin {
    @Inject(method={"onRemove","onPlace"},at=@At("HEAD"),cancellable=true)
    private void celestialNail$boundedCallbacks(Level level, BlockPos pos, BlockState other, boolean moving, CallbackInfo ci) {
        // Level still handles lighting, height maps, persistence, POIs and client notifications.
        // NailWorldOperations explicitly removes obsolete block entities before this point.
        if(NailMutationScope.active()) ci.cancel();
    }
}
