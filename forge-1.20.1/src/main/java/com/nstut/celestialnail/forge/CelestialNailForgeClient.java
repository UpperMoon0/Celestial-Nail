package com.nstut.celestialnail.forge;

import com.nstut.celestialnail.CelestialNail;
import com.nstut.celestialnail.client.CelestialNailRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CelestialNail.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CelestialNailForgeClient {
    private CelestialNailForgeClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CelestialNailForge.NAIL.get(), CelestialNailRenderer::new);
    }
}
