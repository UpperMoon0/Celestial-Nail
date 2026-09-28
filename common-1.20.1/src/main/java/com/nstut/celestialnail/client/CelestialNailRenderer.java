package com.nstut.celestialnail.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CelestialNailRenderer extends EntityRenderer<CelestialNailEntity> {
    private final BlockRenderDispatcher blocks;

    public CelestialNailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(CelestialNailEntity nail, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        float t = nail.tickCount + partialTick;
        float spin = nail.isLaunched() ? t * 14.0F : t * 1.8F;
        float bob = nail.isLaunched() ? 0.0F : (float)Math.sin(t * 0.06F) * 0.18F;
        pose.translate(0.0, bob + 5.225, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        draw(pose, buffers, Blocks.QUARTZ_BLOCK.defaultBlockState(), 0.0F, -2.15F, 0.0F, 0.72F, 4.5F, 0.72F, 0.0F);
        draw(pose, buffers, Blocks.SEA_LANTERN.defaultBlockState(), 0.0F, 0.25F, 0.0F, 1.15F, 1.15F, 1.15F, 45.0F);
        draw(pose, buffers, Blocks.CALCITE.defaultBlockState(), 0.0F, 1.32F, 0.0F, 0.72F, 1.5F, 0.72F, 45.0F);
        draw(pose, buffers, Blocks.AMETHYST_BLOCK.defaultBlockState(), 0.0F, -4.45F, 0.0F, 0.52F, 1.55F, 0.52F, 45.0F);
        draw(pose, buffers, Blocks.QUARTZ_BLOCK.defaultBlockState(), 0.0F, 0.28F, 0.0F, 2.15F, 0.22F, 0.42F, 45.0F);
        draw(pose, buffers, Blocks.QUARTZ_BLOCK.defaultBlockState(), 0.0F, 0.28F, 0.0F, 0.42F, 0.22F, 2.15F, 45.0F);

        pose.popPose();
        super.render(nail, entityYaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, BlockState state, float x, float y, float z, float sx, float sy, float sz, float yRotation) {
        pose.pushPose();
        pose.translate(x - sx * 0.5F, y - sy * 0.5F, z - sz * 0.5F);
        pose.translate(sx * 0.5F, sy * 0.5F, sz * 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(yRotation));
        pose.translate(-sx * 0.5F, -sy * 0.5F, -sz * 0.5F);
        pose.scale(sx, sy, sz);
        this.blocks.renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CelestialNailEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
