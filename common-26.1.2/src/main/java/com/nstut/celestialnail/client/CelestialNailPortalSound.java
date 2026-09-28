package com.nstut.celestialnail.client;

import com.nstut.celestialnail.CelestialNailSounds;
import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Stream once on summon, anchored to the rift, and fade out with its closing animation. */
public final class CelestialNailPortalSound extends AbstractTickableSoundInstance {
    private final CelestialNailEntity nail;
    private CelestialNailPortalSound(CelestialNailEntity nail) {
        super(CelestialNailSounds.PORTAL_OPEN, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.nail=nail;
        this.looping=false;
        this.x=nail.getX(); this.y=nail.portalY(); this.z=nail.getZ();
        this.volume=.85F;
    }
    public static void tickEntity(CelestialNailEntity nail) {
        if (nail.portalSoundStarted) return;
        nail.portalSoundStarted=true;
        // Tracking an old nail must not restart the opening recording.
        if (nail.summonAge(0) >= 0 && nail.summonAge(0) < 30 && !nail.isLaunched())
            Minecraft.getInstance().getSoundManager().play(new CelestialNailPortalSound(nail));
    }
    @Override
    public void tick() {
        if (nail.isRemoved() || (nail.isLaunched() && nail.launchAge(0) >= CelestialNailVisuals.CLOSE_TICKS)) {
            stop(); return;
        }
        float fade=nail.isLaunched() ? 1-CelestialNailVisuals.smooth(nail.launchAge(0)/CelestialNailVisuals.CLOSE_TICKS) : 1;
        this.volume=.85F*fade;
    }
}
