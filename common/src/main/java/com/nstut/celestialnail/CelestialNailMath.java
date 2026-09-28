package com.nstut.celestialnail;

/** Pure impact-wave math shared by every Minecraft version. */
public final class CelestialNailMath {
    private CelestialNailMath() {}

    public static int targetRadius(float power) {
        return (int) Math.ceil(power);
    }

    /** Returns true when an integer block offset belongs to the radius-r spherical shell. */
    public static boolean isInShell(int dx, int dy, int dz, int radius) {
        if (radius < 0) return false;
        long distSq = (long) dx * dx + (long) dy * dy + (long) dz * dz;
        long outerSq = (long) radius * radius;
        if (distSq > outerSq) return false;
        if (radius == 0) return true;
        long inner = radius - 1L;
        return distSq > inner * inner;
    }

    /** Maximum |z| inside the radius-r sphere for one x/y column, or -1 outside the sphere. */
    public static int outerZExtent(int dx, int dy, int radius) {
        if (radius < 0) return -1;
        long remaining = (long) radius * radius - (long) dx * dx - (long) dy * dy;
        return remaining < 0L ? -1 : floorSqrt(remaining);
    }

    /** Maximum |z| inside the previous (r-1) sphere for one x/y column, or -1 if none. */
    public static int innerZExtent(int dx, int dy, int radius) {
        if (radius <= 0) return -1;
        int innerRadius = radius - 1;
        long remaining = (long) innerRadius * innerRadius - (long) dx * dx - (long) dy * dy;
        return remaining < 0L ? -1 : floorSqrt(remaining);
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
    private static int floorSqrt(long value) {
        int root = (int) Math.sqrt(value);
        while ((long) (root + 1) * (root + 1) <= value) root++;
        while ((long) root * root > value) root--;
        return root;
    }
}