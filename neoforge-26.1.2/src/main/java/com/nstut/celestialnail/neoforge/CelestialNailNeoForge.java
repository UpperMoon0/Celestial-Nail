package com.nstut.celestialnail.neoforge;

import com.nstut.celestialnail.CelestialNail;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CelestialNail.MOD_ID)
public final class CelestialNailNeoForge {
    public CelestialNailNeoForge(IEventBus modBus) {
        CelestialNail.init();
    }
}
