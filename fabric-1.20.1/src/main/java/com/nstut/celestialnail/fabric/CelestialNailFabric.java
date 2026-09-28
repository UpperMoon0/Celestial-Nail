package com.nstut.celestialnail.fabric;

import com.nstut.celestialnail.CelestialNail;
import net.fabricmc.api.ModInitializer;

public final class CelestialNailFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CelestialNail.init();
    }
}
