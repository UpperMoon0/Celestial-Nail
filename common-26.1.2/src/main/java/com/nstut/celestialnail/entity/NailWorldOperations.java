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
        // The newer engine has a dedicated block-entity side-effect suppression flag.
        return level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
    }

    public static boolean forceChunk(ServerLevel level, ChunkPos chunk) {
        // TicketStorage persists FORCED tickets; unlike ServerLevel this does not call getChunk.
        return level.getChunkSource().updateChunkForced(chunk, true);
    }
}
