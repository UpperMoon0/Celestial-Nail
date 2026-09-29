package com.nstut.celestialnail.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Version adapter for vanilla mutation and persistent, nonblocking forced-chunk requests. */
public final class NailWorldOperations {
    private NailWorldOperations() {}

    public static boolean replaceWithoutDrops(ServerLevel level, BlockPos pos, BlockState next) {
        if (!level.isLoaded(pos)) return false;
        BlockState old = level.getBlockState(pos);
        if (old == next) return false;
        try (var ignored = com.nstut.celestialnail.NailMutationScope.enter()) {
            // Do not invoke inventory callbacks: they may read neighbors or emit drops.
            // Unregister the obsolete BE/ticker/listener; same-block state changes retain it.
            if (old.hasBlockEntity() && old.getBlock() != next.getBlock()) level.removeBlockEntity(pos);
            return level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

    public static BlockState reconcileBoundary(ServerLevel level, BlockPos pos, BlockState state) {
        try (var ignored = com.nstut.celestialnail.NailMutationScope.enter()) {
            // Some updateShape implementations return unchanged states but queue a destructive
            // survival tick. Resolve survival now, inside the caller's change/scan budget.
            if (state.getBlock() instanceof net.minecraft.world.level.block.FallingBlock
                    && net.minecraft.world.level.block.FallingBlock.isFree(level.getBlockState(pos.below())))
                return state.getFluidState().createLegacyBlock();
            if (!state.canSurvive(level, pos)) return state.getFluidState().createLegacyBlock();
            if (state.is(net.minecraft.world.level.block.Blocks.SCAFFOLDING)) {
                int distance=net.minecraft.world.level.block.ScaffoldingBlock.getDistance(level,pos);
                var next=state.setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE,distance)
                        .setValue(net.minecraft.world.level.block.ScaffoldingBlock.BOTTOM,
                                distance>0 && !level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SCAFFOLDING));
                if (!state.getFluidState().isEmpty()) level.scheduleTick(pos,state.getFluidState().getType(),state.getFluidState().getType().getTickDelay(level));
                return next;
            }
            if (state.getBlock() instanceof LeavesBlock) {
                // Vanilla updateShape only schedules this calculation; the scoped scheduler
                // suppresses that tick. Resolve the same six-neighbor distance here instead.
                // The caller has checked those chunks and budgets the resulting replacement.
                int distance=7;
                for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                    var neighbor=level.getBlockState(pos.relative(direction));
                    distance=Math.min(distance,LeavesBlock.getOptionalDistanceAt(neighbor).orElse(7)+1);
                    if (distance==1) break;
                }
                if (!state.getFluidState().isEmpty()) level.scheduleTick(pos,state.getFluidState().getType(),state.getFluidState().getType().getTickDelay(level));
                // Do not leave distance-7 natural leaves for random decay, which could drop
                // items outside the allowance. Persistent leaves retain their other properties.
                if (distance==7 && !state.getValue(LeavesBlock.PERSISTENT))
                    return state.getFluidState().createLegacyBlock();
                return state.setValue(LeavesBlock.DISTANCE,distance);
            }
            // The scoped scheduler drops deferred block effects, but allows external fluid flow.
            return Block.updateFromNeighbourShapes(state, level, pos);
        }
    }

    public static boolean forceChunk(ServerLevel level, ChunkPos chunk) {
        // Preserve the vanilla saved set and ticket semantics, but do not synchronously getChunk.
        var saved = level.getDataStorage().computeIfAbsent(net.minecraft.world.level.ForcedChunksSavedData::load, net.minecraft.world.level.ForcedChunksSavedData::new, "chunks");
        if (!saved.getChunks().add(chunk.toLong())) return false;
        saved.setDirty();
        level.getChunkSource().updateChunkForced(chunk, true);
        return true;
    }
}
