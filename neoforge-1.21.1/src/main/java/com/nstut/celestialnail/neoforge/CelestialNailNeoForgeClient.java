package com.nstut.celestialnail.neoforge;

import com.nstut.celestialnail.CelestialNail;
import com.nstut.celestialnail.client.CelestialNailRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CelestialNail.MOD_ID, value = Dist.CLIENT)
public final class CelestialNailNeoForgeClient {
    private CelestialNailNeoForgeClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CelestialNailNeoForge.NAIL.get(), CelestialNailRenderer::new);
    }
}
