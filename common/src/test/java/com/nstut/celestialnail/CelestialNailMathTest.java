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
}