package com.nstut.celestialnail.neoforge;

import com.nstut.celestialnail.CelestialNail;
import com.nstut.celestialnail.command.CelestialNailCommands;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CelestialNail.MOD_ID)
public final class CelestialNailNeoForge {
    public static final ResourceKey<EntityType<?>> NAIL_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CelestialNail.MOD_ID, "celestial_nail"));
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, CelestialNail.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<CelestialNailEntity>> NAIL = ENTITIES.register("celestial_nail", () ->
        EntityType.Builder.<CelestialNailEntity>of(CelestialNailEntity::new, MobCategory.MISC)
            .noLootTable().sized(1.5F, 8.0F).clientTrackingRange(16).updateInterval(1)
            .build(NAIL_KEY));

    public CelestialNailNeoForge(IEventBus modBus) {
        CelestialNail.init();
        ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CelestialNailCommands.register(event.getDispatcher(), NAIL::get);
    }
}