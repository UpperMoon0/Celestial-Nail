package com.nstut.celestialnail.client;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Vanilla entity pipelines: shader loaders can classify the material and consume NEW_ENTITY attributes. */
public final class CelestialNailSkyRender {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("celestial_nail", "textures/entity/celestial_nail.png");
    public static final RenderType TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);
    public static final RenderType EFFECT = RenderTypes.entityTranslucentEmissive(TEXTURE);
    private CelestialNailSkyRender() {}
}
