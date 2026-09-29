package com.nstut.celestialnail.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Version adapter for vanilla mutation and persistent, nonblocking forced-chunk requests. */
public final class NailWorldOperations {
    private NailWorldOperations() {}

    public static boolean replaceWithoutDrops(ServerLevel level, BlockPos pos, BlockState next) {
        BlockState old = level.getBlockState(pos);
        if (old == next) return false;
        // Older LevelChunk still calls onRemove regardless of UPDATE_SUPPRESS_DROPS.
        // Remove the inventory-bearing BE first; retain the normal block cleanup callback.
        // Same-block state changes (e.g. waterlogging/shape) must preserve the BE.
        if (old.hasBlockEntity() && old.getBlock() != next.getBlock()) {
            net.minecraft.world.Clearable.tryClear(level.getBlockEntity(pos));
            level.removeBlockEntity(pos);
        }
        return level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
    }

    public static boolean forceChunk(ServerLevel level, ChunkPos chunk) {
        // Preserve the vanilla saved set and ticket semantics, but do not synchronously getChunk.
        var saved = level.getDataStorage().computeIfAbsent(net.minecraft.world.level.ForcedChunksSavedData.factory(), "chunks");
        if (!saved.getChunks().add(chunk.toLong())) return false;
        saved.setDirty();
        level.getChunkSource().updateChunkForced(chunk, true);
        return true;
    }
}
