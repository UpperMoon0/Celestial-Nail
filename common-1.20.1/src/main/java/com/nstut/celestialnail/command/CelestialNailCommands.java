package com.nstut.celestialnail.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.entity.CelestialNailIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public final class CelestialNailCommands {
    private CelestialNailCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Supplier<EntityType<CelestialNailEntity>> entityType) {
        dispatcher.register(Commands.literal("celestialnail")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("summon")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(ctx -> summon(ctx.getSource(), entityType.get(), StringArgumentType.getString(ctx, "id"), Vec3Argument.getVec3(ctx, "pos"), CelestialNailEntity.DEFAULT_POWER, 1.0F))
                        .then(Commands.argument("power", FloatArgumentType.floatArg(CelestialNailEntity.MIN_POWER, CelestialNailEntity.MAX_POWER))
                            .executes(ctx -> summon(ctx.getSource(), entityType.get(), StringArgumentType.getString(ctx, "id"), Vec3Argument.getVec3(ctx, "pos"), FloatArgumentType.getFloat(ctx, "power"), 1.0F))
                            .then(Commands.argument("scale", FloatArgumentType.floatArg(CelestialNailVisuals.MIN_SCALE, CelestialNailVisuals.MAX_SCALE))
                                .executes(ctx -> summon(ctx.getSource(), entityType.get(), StringArgumentType.getString(ctx, "id"), Vec3Argument.getVec3(ctx, "pos"), FloatArgumentType.getFloat(ctx, "power"), FloatArgumentType.getFloat(ctx, "scale"))))))))
            .then(Commands.literal("launch")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> launch(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("list").executes(ctx -> list(ctx.getSource()))));
    }

    private static int summon(CommandSourceStack source, EntityType<CelestialNailEntity> type, String id, Vec3 pos, float power, float scale) {
        if (find(source, id) != null) {
            source.sendFailure(Component.literal("Celestial Nail '" + id + "' already exists"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        CelestialNailEntity nail = type.create(level);
        if (nail == null) {
            source.sendFailure(Component.literal("Failed to create Celestial Nail"));
            return 0;
        }
        nail.configure(id, power);
        nail.setPos(pos.x, pos.y, pos.z);
        nail.beginSummoning(scale);
        if (!level.addFreshEntity(nail)) {
            source.sendFailure(Component.literal("Failed to add Celestial Nail to the world"));
            return 0;
        }
        CelestialNailIndex.register(source.getServer(), nail);
        nail.forceOwnChunk(level);
        source.sendSuccess(() -> Component.literal("Summoned Celestial Nail '" + id + "' at " + fmt(pos) + " with power/radius " + power + ", height " + nail.nailHeight() + " blocks (scale " + scale + ")"), true);
        return 1;
    }

    private static int launch(CommandSourceStack source, String id) {
        CelestialNailEntity nail = find(source, id);
        if (nail == null) {
            source.sendFailure(Component.literal("Unknown Celestial Nail '" + id + "'"));
            return 0;
        }
        if (!nail.launch()) {
            source.sendFailure(Component.literal("Celestial Nail '" + id + "' has already launched or is still emerging"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Launched Celestial Nail '" + id + "'"), true);
        return 1;
    }

    private static int remove(CommandSourceStack source, String id) {
        CelestialNailEntity nail = find(source, id);
        if (nail == null) {
            source.sendFailure(Component.literal("Unknown Celestial Nail '" + id + "'"));
            return 0;
        }
        nail.discard();
        source.sendSuccess(() -> Component.literal("Removed Celestial Nail '" + id + "'"), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        List<String> ids = new ArrayList<>();
        for (CelestialNailEntity nail : CelestialNailIndex.all(source.getServer())) {
            ServerLevel level = (ServerLevel) nail.level();
            ids.add(nail.nailId() + " @ " + level.dimension().location() + " " + fmt(nail.position()));
        }
        source.sendSuccess(() -> Component.literal(ids.isEmpty() ? "No Celestial Nails are active" : "Celestial Nails: " + String.join(", ", ids)), false);
        return ids.size();
    }

    private static CelestialNailEntity find(CommandSourceStack source, String id) {
        return CelestialNailIndex.find(source.getServer(), id);
    }

    private static String fmt(Vec3 pos) {
        return String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f", pos.x, pos.y, pos.z);
    }
}
