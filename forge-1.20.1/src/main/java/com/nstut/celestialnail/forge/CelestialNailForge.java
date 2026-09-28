package com.nstut.celestialnail.forge;

import com.nstut.celestialnail.CelestialNail;
import com.nstut.celestialnail.CelestialNailSounds;
import net.minecraft.sounds.SoundEvent;
import com.nstut.celestialnail.command.CelestialNailCommands;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CelestialNail.MOD_ID)
public final class CelestialNailForge {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CelestialNail.MOD_ID);
    static { SOUNDS.register("portal_open", () -> CelestialNailSounds.PORTAL_OPEN); }
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CelestialNail.MOD_ID);
    public static final RegistryObject<EntityType<CelestialNailEntity>> NAIL = ENTITIES.register("celestial_nail", () ->
        EntityType.Builder.<CelestialNailEntity>of(CelestialNailEntity::new, MobCategory.MISC)
            .sized(1.5F, 8.0F).clientTrackingRange(64).updateInterval(1)
            .build(CelestialNail.MOD_ID + ":celestial_nail"));

    public CelestialNailForge() {
        CelestialNail.init();
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CelestialNailCommands.register(event.getDispatcher(), NAIL::get);
    }
}
