package com.nstut.celestialnail.neoforge;

import com.nstut.celestialnail.CelestialNail;
import com.nstut.celestialnail.CelestialNailSounds;
import net.minecraft.sounds.SoundEvent;
import com.nstut.celestialnail.command.CelestialNailCommands;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CelestialNail.MOD_ID)
public final class CelestialNailNeoForge {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CelestialNail.MOD_ID);
    static { SOUNDS.register("portal_open", () -> CelestialNailSounds.PORTAL_OPEN); }
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, CelestialNail.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<CelestialNailEntity>> NAIL = ENTITIES.register("celestial_nail", () ->
        EntityType.Builder.<CelestialNailEntity>of(CelestialNailEntity::new, MobCategory.MISC)
            .sized(1.5F, 8.0F).clientTrackingRange(64).updateInterval(1)
            .build(CelestialNail.MOD_ID + ":celestial_nail"));

    public CelestialNailNeoForge(IEventBus modBus) {
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CelestialNailCommands.register(event.getDispatcher(), NAIL::get);
    }
}
