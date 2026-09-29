package com.nstut.celestialnail.client;

import com.nstut.celestialnail.CelestialNailSounds;
import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Stream once on summon with broad horizontal reach, fading with the closing animation. */
public final class CelestialNailPortalSound extends AbstractTickableSoundInstance {
    private final CelestialNailEntity nail;
    private CelestialNailPortalSound(CelestialNailEntity nail) {
        super(CelestialNailSounds.PORTAL_OPEN, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.nail=nail;
        this.looping=false;
        this.relative=true; this.attenuation=SoundInstance.Attenuation.NONE;
        this.x=this.y=this.z=0;
        this.volume=.85F*audibility();
    }
    private float audibility() {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return 0;
        // The high rift must remain audible from the ground, independently of visual fading.
        double distance=Math.hypot(nail.getX()-mc.player.getX(),nail.getZ()-mc.player.getZ());
        double reach=Math.max(512,mc.options.getEffectiveRenderDistance()*32.0);
        return 1-CelestialNailVisuals.smooth((float)((distance-reach*.5)/(reach*.5)));
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
        if(nail.isCrumbling())fade*=1-CelestialNailVisuals.smooth(nail.crumbleAge(0)/15);
        this.volume=.85F*fade*audibility();
    }
}
