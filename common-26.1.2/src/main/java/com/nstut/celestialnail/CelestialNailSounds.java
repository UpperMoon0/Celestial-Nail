package com.nstut.celestialnail;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class CelestialNailSounds {
    public static final Identifier PORTAL_ID = Identifier.fromNamespaceAndPath("celestial_nail", "portal_open");
    public static final SoundEvent PORTAL_OPEN = SoundEvent.createVariableRangeEvent(PORTAL_ID);
    private CelestialNailSounds() {}
}
