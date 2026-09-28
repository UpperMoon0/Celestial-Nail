package com.nstut.celestialnail.client;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class CelestialNailRenderState extends EntityRenderState {
    public boolean launched;
    public final BlockModelRenderState shaft = new BlockModelRenderState();
    public final BlockModelRenderState core = new BlockModelRenderState();
    public final BlockModelRenderState cap = new BlockModelRenderState();
    public final BlockModelRenderState tip = new BlockModelRenderState();
    public final BlockModelRenderState armX = new BlockModelRenderState();
    public final BlockModelRenderState armZ = new BlockModelRenderState();
}
