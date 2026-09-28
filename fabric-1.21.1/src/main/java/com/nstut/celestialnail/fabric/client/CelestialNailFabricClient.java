package com.nstut.celestialnail.fabric.client;

import com.nstut.celestialnail.client.CelestialNailRenderer;
import com.nstut.celestialnail.fabric.CelestialNailFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class CelestialNailFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(CelestialNailFabric.NAIL, CelestialNailRenderer::new);
    }
}
