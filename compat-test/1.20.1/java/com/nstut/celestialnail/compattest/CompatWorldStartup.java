package com.nstut.celestialnail.compattest;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
final class CompatWorldStartup {
 static void create(Minecraft mc,String name,LevelSettings settings) {
  mc.createWorldOpenFlows().createFreshLevel(name,settings,new WorldOptions(8675309,false,false),
   registry->registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
 }
}
