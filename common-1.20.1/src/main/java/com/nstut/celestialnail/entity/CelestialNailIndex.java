package com.nstut.celestialnail.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Ephemeral per-server index used to make command chaining deterministic.
 * Persistent truth remains the saved, force-loaded entity itself.
 */
public final class CelestialNailIndex {
    private static final Map<MinecraftServer, Map<String, CelestialNailEntity>> BY_SERVER = new WeakHashMap<>();

    private CelestialNailIndex() {}

    public static synchronized void register(MinecraftServer server, CelestialNailEntity nail) {
        BY_SERVER.computeIfAbsent(server, ignored -> new LinkedHashMap<>()).put(nail.nailId(), nail);
    }

    public static synchronized void unregister(MinecraftServer server, CelestialNailEntity nail) {
        Map<String, CelestialNailEntity> indexed = BY_SERVER.get(server);
        if (indexed == null) return;
        indexed.remove(nail.nailId(), nail);
        if (indexed.isEmpty()) BY_SERVER.remove(server);
    }

    public static synchronized CelestialNailEntity find(MinecraftServer server, String id) {
        Map<String, CelestialNailEntity> indexed = BY_SERVER.get(server);
        if (indexed != null) {
            CelestialNailEntity nail = indexed.get(id);
            if (isValid(server, nail)) return nail;
            if (nail != null) indexed.remove(id);
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CelestialNailEntity nail && id.equals(nail.nailId()) && !nail.isRemoved()) {
                    register(server, nail);
                    return nail;
                }
            }
        }
        return null;
    }

    public static synchronized List<CelestialNailEntity> all(MinecraftServer server) {
        LinkedHashMap<String, CelestialNailEntity> found = new LinkedHashMap<>();
        Map<String, CelestialNailEntity> indexed = BY_SERVER.get(server);
        if (indexed != null) {
            Iterator<Map.Entry<String, CelestialNailEntity>> iterator = indexed.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CelestialNailEntity> entry = iterator.next();
                if (isValid(server, entry.getValue())) {
                    found.put(entry.getKey(), entry.getValue());
                } else {
                    iterator.remove();
                }
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CelestialNailEntity nail && !nail.isRemoved()) {
                    found.put(nail.nailId(), nail);
                    BY_SERVER.computeIfAbsent(server, ignored -> new LinkedHashMap<>()).put(nail.nailId(), nail);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private static boolean isValid(MinecraftServer server, CelestialNailEntity nail) {
        return nail != null
            && !nail.isRemoved()
            && nail.level() instanceof ServerLevel level
            && level.getServer() == server;
    }
}