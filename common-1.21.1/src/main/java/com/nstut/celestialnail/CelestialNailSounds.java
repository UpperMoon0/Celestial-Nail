package com.nstut.celestialnail;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class CelestialNailSounds {
    public static final ResourceLocation PORTAL_ID = ResourceLocation.fromNamespaceAndPath("celestial_nail", "portal_open");
    public static final SoundEvent PORTAL_OPEN = SoundEvent.createVariableRangeEvent(PORTAL_ID);
    private CelestialNailSounds() {}
}
