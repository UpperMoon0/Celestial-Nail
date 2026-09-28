package com.nstut.celestialnail.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public final class CelestialNailRenderer extends EntityRenderer<CelestialNailEntity, CelestialNailRenderState> {
    private static final BlockDisplayContext BLOCK_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModels;

    public CelestialNailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModels = context.getBlockModelResolver();
        this.shadowRadius = 0.0F;
    }

    @Override
    protected int getSkyLightLevel(CelestialNailEntity entity, BlockPos pos) { return 15; }

    @Override
    protected int getBlockLightLevel(CelestialNailEntity entity, BlockPos pos) { return 15; }

    @Override
    public CelestialNailRenderState createRenderState() {
        return new CelestialNailRenderState();
    }

    @Override
    public void extractRenderState(CelestialNailEntity entity, CelestialNailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.launched = entity.isLaunched();
        this.blockModels.update(state.shaft, Blocks.QUARTZ_BLOCK.defaultBlockState(), BLOCK_CONTEXT);
        this.blockModels.update(state.core, Blocks.SEA_LANTERN.defaultBlockState(), BLOCK_CONTEXT);
        this.blockModels.update(state.cap, Blocks.CALCITE.defaultBlockState(), BLOCK_CONTEXT);
        this.blockModels.update(state.tip, Blocks.AMETHYST_BLOCK.defaultBlockState(), BLOCK_CONTEXT);
        this.blockModels.update(state.armX, Blocks.QUARTZ_BLOCK.defaultBlockState(), BLOCK_CONTEXT);
        this.blockModels.update(state.armZ, Blocks.QUARTZ_BLOCK.defaultBlockState(), BLOCK_CONTEXT);
    }

    @Override
    public void submit(CelestialNailRenderState state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera) {
        pose.pushPose();
        float spin = state.launched ? state.ageInTicks * 14.0F : state.ageInTicks * 1.8F;
        float bob = state.launched ? 0.0F : (float)Math.sin(state.ageInTicks * 0.06F) * 0.18F;
        pose.translate(0.0F, bob + 5.225F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        submitBlock(state.shaft, pose, nodes, state, 0.0F, -2.15F, 0.0F, 0.72F, 4.5F, 0.72F, 0.0F);
        submitBlock(state.core, pose, nodes, state, 0.0F, 0.25F, 0.0F, 1.15F, 1.15F, 1.15F, 45.0F);
        submitBlock(state.cap, pose, nodes, state, 0.0F, 1.32F, 0.0F, 0.72F, 1.5F, 0.72F, 45.0F);
        submitBlock(state.tip, pose, nodes, state, 0.0F, -4.45F, 0.0F, 0.52F, 1.55F, 0.52F, 45.0F);
        submitBlock(state.armX, pose, nodes, state, 0.0F, 0.28F, 0.0F, 2.15F, 0.22F, 0.42F, 45.0F);
        submitBlock(state.armZ, pose, nodes, state, 0.0F, 0.28F, 0.0F, 0.42F, 0.22F, 2.15F, 45.0F);

        pose.popPose();
        super.submit(state, pose, nodes, camera);
    }

    private static void submitBlock(BlockModelRenderState model, PoseStack pose, SubmitNodeCollector nodes, CelestialNailRenderState state,
                                    float x, float y, float z, float sx, float sy, float sz, float yRotation) {
        pose.pushPose();
        pose.translate(x - sx * 0.5F, y - sy * 0.5F, z - sz * 0.5F);
        pose.translate(sx * 0.5F, sy * 0.5F, sz * 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(yRotation));
        pose.translate(-sx * 0.5F, -sy * 0.5F, -sz * 0.5F);
        pose.scale(sx, sy, sz);
        model.submit(pose, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }
}
