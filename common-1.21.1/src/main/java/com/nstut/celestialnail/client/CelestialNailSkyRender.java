package com.nstut.celestialnail.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Vanilla entity pipelines: shader loaders can classify the material and consume NEW_ENTITY attributes. */
public final class CelestialNailSkyRender {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("celestial_nail", "textures/entity/celestial_nail.png");
    public static final RenderType TYPE = RenderType.entityTranslucent(TEXTURE);
    public static final RenderType EFFECT = RenderType.entityTranslucentEmissive(TEXTURE);
    private CelestialNailSkyRender() {}
}
