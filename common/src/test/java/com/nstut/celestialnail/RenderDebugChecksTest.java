package com.nstut.celestialnail;

import com.nstut.celestialnail.client.RenderDebugChecks;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static com.nstut.celestialnail.client.RenderDebugChecks.Result.*;
import static org.junit.jupiter.api.Assertions.*;

class RenderDebugChecksTest {
    @Test void submittedMeshCannotPassWhenShaderPackSuppressesCustomShader() {
        assertEquals(NO_SHADER_APPLY, RenderDebugChecks.evaluate(60, 4096, 1, 0, false, -1));
        assertEquals(WRONG_PROGRAM, RenderDebugChecks.evaluate(60, 4096, 1, 60, true, 100));
        assertEquals(NO_PIXEL_CHANGE, RenderDebugChecks.evaluate(60, 4096, 1, 60, false, 0));
        assertEquals(PIXELS_CHANGED, RenderDebugChecks.evaluate(60, 4096, 1, 60, false, 100));
        // A draw call alone must never be described as verified visible pixels.
        assertEquals(DRAW_OBSERVED, RenderDebugChecks.evaluate(60, 4096, 1, 60, false, -1));
    }
    @Test void identifiesEarlierVisibilityFailuresBeforeShaderFailures() {
        assertEquals(NO_RENDER, RenderDebugChecks.evaluate(0, 0, 0, 0, false, -1));
        assertEquals(NO_VERTICES, RenderDebugChecks.evaluate(1, 0, 1, 0, false, -1));
        assertEquals(TRANSPARENT, RenderDebugChecks.evaluate(1, 400, 0, 1, false, 0));
        assertEquals(TRANSPARENT, RenderDebugChecks.evaluate(1, 400, Float.NaN, 1, false, 0));
    }
    @Test void pixelProbeDetectsRgbWritesButRejectsAlphaOnlyAndRoundingNoise() {
        var before = ByteBuffer.wrap(new byte[]{0,0,0,0, 127,127,127,0, (byte)255,0,0,0});
        var after = ByteBuffer.wrap(new byte[]{0,0,0,(byte)255, (byte)128,127,127,0, 0,0,0,0});
        assertEquals(1, RenderDebugChecks.changedPixels(before, after, 12));
        assertEquals(0, before.position());
        assertEquals(0, after.position());
        assertEquals(0, RenderDebugChecks.changedPixels(before, before, 12));
        assertThrows(IllegalArgumentException.class, () -> RenderDebugChecks.changedPixels(before, after, 13));
    }
}
