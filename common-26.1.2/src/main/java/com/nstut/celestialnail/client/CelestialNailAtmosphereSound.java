package com.nstut.celestialnail.client;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
/** Listener-relative cinematic mix: height never makes an overhead rift inaudible. */
final class CelestialNailAtmosphereSound extends AbstractTickableSoundInstance {
    float gain;
    CelestialNailAtmosphereSound(String name,boolean repeat) {
        super(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("celestial_nail",name)),SoundSource.AMBIENT,SoundInstance.createUnseededRandom());
        looping=repeat; delay=0; relative=true; attenuation=SoundInstance.Attenuation.NONE; volume=1;
    }
    public boolean canStartSilent() {return true;}
    public void tick() {volume=Math.max(0,Math.min(1,gain));}
    void finish() {stop();}
}
