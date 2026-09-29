package com.nstut.celestialnail;

/** Pure impact-wave math shared by every Minecraft version. */
public final class CelestialNailMath {
    private CelestialNailMath() {}

    public static int targetRadius(float power) {
        return (int) Math.ceil(power);
    }

    /** Vanilla-compatible packed chunk key, shared by every supported Minecraft version. */
    public static long packChunk(int chunkX, int chunkZ) {
        return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
    }

    public static int unpackChunkX(long packed) {
        return (int) packed;
    }

    public static int unpackChunkZ(long packed) {
        return (int) (packed >>> 32);
    }

    /** True when a 16x16 chunk footprint intersects an impact sphere's horizontal projection. */
    public static boolean chunkIntersectsHorizontalRadius(int chunkX, int chunkZ, int centerX, int centerZ, int radius) {
        long minX = (long) chunkX * 16L;
        long minZ = (long) chunkZ * 16L;
        long maxX = minX + 15L;
        long maxZ = minZ + 15L;
        long closestX = Math.max(minX, Math.min(maxX, (long) centerX));
        long closestZ = Math.max(minZ, Math.min(maxZ, (long) centerZ));
        long dx = (long) centerX - closestX;
        long dz = (long) centerZ - closestZ;
        return dx * dx + dz * dz <= (long) radius * radius;
    }
}
