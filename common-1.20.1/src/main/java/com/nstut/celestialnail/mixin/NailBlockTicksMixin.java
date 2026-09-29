package com.nstut.celestialnail.mixin;

import com.nstut.celestialnail.NailMutationScope;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelTicks.class)
public abstract class NailBlockTicksMixin {
    @Inject(method="schedule",at=@At("HEAD"),cancellable=true)
    private void celestialNail$noDeferredBlockCascade(ScheduledTick<?> tick, CallbackInfo ci) {
        // Water/lava ticks remain permitted. Block survival is reconciled by the bounded pass.
        if(NailMutationScope.active() && tick.type() instanceof Block) ci.cancel();
    }
}
