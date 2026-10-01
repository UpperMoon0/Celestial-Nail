package com.nstut.celestialnail.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Vanilla entity pipelines: shader loaders can classify the material and consume NEW_ENTITY attributes. */
public final class CelestialNailSkyRender {
    public static String debugShader() { return "entity_translucent_emissive"; }
    private static final ResourceLocation TEXTURE = new ResourceLocation("celestial_nail", "textures/entity/celestial_nail.png");
    public static final RenderType TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    public static final RenderType EFFECT = RenderType.entityTranslucentEmissive(TEXTURE);
    private CelestialNailSkyRender() {}
}
