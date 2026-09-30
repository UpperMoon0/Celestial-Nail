package com.nstut.celestialnail.fabric;

import com.nstut.celestialnail.CelestialNail;
import com.nstut.celestialnail.CelestialNailSounds;
import net.minecraft.sounds.SoundEvent;
import com.nstut.celestialnail.command.CelestialNailCommands;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class CelestialNailFabric implements ModInitializer {
    public static final EntityType<CelestialNailEntity> NAIL = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(CelestialNail.MOD_ID, "celestial_nail"),
        EntityType.Builder.<CelestialNailEntity>of(CelestialNailEntity::new, MobCategory.MISC)
            .sized(1.5F, 8.0F).clientTrackingRange(64).updateInterval(1)
            .build(CelestialNail.MOD_ID + ":celestial_nail")
    );

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, CelestialNailSounds.PORTAL_ID, CelestialNailSounds.PORTAL_OPEN);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CelestialNailCommands.register(dispatcher, () -> NAIL));
    }
}
