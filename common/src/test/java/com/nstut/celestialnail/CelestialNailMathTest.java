package com.nstut.celestialnail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CelestialNailMathTest {
    @Test
    void powerRoundsUpToDestructionRadius() {
        assertEquals(32, CelestialNailMath.targetRadius(32.0F));
        assertEquals(33, CelestialNailMath.targetRadius(32.01F));
    }

    @Test
    void everyIntegerOffsetBelongsToExactlyOneCeilingDistanceShell() {
        for (int x = -12; x <= 12; x++) {
            for (int y = -12; y <= 12; y++) {
                for (int z = -12; z <= 12; z++) {
                    int expected = (int) Math.ceil(Math.sqrt((double)x * x + (double)y * y + (double)z * z));
                    int matches = 0;
                    for (int r = 0; r <= expected + 1; r++) {
                        if (CelestialNailMath.isInShell(x, y, z, r)) matches++;
                    }
                    assertEquals(1, matches, "offset " + x + "," + y + "," + z);
                    assertTrue(CelestialNailMath.isInShell(x, y, z, expected));
                }
            }
        }
    }

    @Test
    void shellColumnExtentsExactlyMatchShellPredicate() {
        for (int radius = 0; radius <= 16; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    int outer = CelestialNailMath.outerZExtent(x, y, radius);
                    int inner = CelestialNailMath.innerZExtent(x, y, radius);
                    for (int z = -radius; z <= radius; z++) {
                        boolean generated = outer >= 0 && Math.abs(z) <= outer && (inner < 0 || Math.abs(z) > inner);
                        assertEquals(CelestialNailMath.isInShell(x, y, z, radius), generated,
                            "r=" + radius + " offset=" + x + "," + y + "," + z);
                    }
                }
            }
        }
    }

    @Test
    void packedChunkKeysRoundTripSignedCoordinates() {
        int[][] samples = {{0, 0}, {-1, 2}, {17, -31}, {Integer.MIN_VALUE, Integer.MAX_VALUE}};
        for (int[] sample : samples) {
            long packed = CelestialNailMath.packChunk(sample[0], sample[1]);
            assertEquals(sample[0], CelestialNailMath.unpackChunkX(packed));
            assertEquals(sample[1], CelestialNailMath.unpackChunkZ(packed));
        }
    }

    @Test
    void chunkIntersectionUsesActualChunkFootprint() {
        assertTrue(CelestialNailMath.chunkIntersectsHorizontalRadius(0, 0, 0, 0, 0));
        assertFalse(CelestialNailMath.chunkIntersectsHorizontalRadius(-1, 0, 0, 0, 0));
        assertTrue(CelestialNailMath.chunkIntersectsHorizontalRadius(-1, 0, 0, 0, 1));
        assertTrue(CelestialNailMath.chunkIntersectsHorizontalRadius(1, 0, 8, 8, 8));
        assertFalse(CelestialNailMath.chunkIntersectsHorizontalRadius(2, 0, 8, 8, 8));
    }
    @Test
    void shellExcludesInnerAndOuterPoints() {
        assertFalse(CelestialNailMath.isInShell(1, 0, 0, 3));
        assertTrue(CelestialNailMath.isInShell(3, 0, 0, 3));
        assertFalse(CelestialNailMath.isInShell(4, 0, 0, 3));
    }
}