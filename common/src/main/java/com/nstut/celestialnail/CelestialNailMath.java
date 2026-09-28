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

    private static int floorSqrt(long value) {
        int root = (int) Math.sqrt(value);
        while ((long) (root + 1) * (root + 1) <= value) root++;
        while ((long) root * root > value) root--;
        return root;
    }
}