package com.nstut.celestialnail.forge;

import com.nstut.celestialnail.CelestialNail;
import net.minecraftforge.fml.common.Mod;

@Mod(CelestialNail.MOD_ID)
public final class CelestialNailForge {
    public CelestialNailForge() {
        CelestialNail.init();
    }
}
