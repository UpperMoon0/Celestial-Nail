package com.nstut.celestialnail.client;

import java.nio.ByteBuffer;

/** Failures observable in an explicitly visible, fully emerged render-test scene. */
public final class RenderDebugChecks {
    public enum Result {
        NO_RENDER, NO_VERTICES, TRANSPARENT, NO_SHADER_APPLY, WRONG_PROGRAM,
        NO_PIXEL_CHANGE, PIXELS_CHANGED, DRAW_OBSERVED
    }
    private RenderDebugChecks() {}

    public static Result evaluate(long renders, long vertices, float maxAlpha, long applies,
                                  boolean programMismatch, int changedPixels) {
        if (renders == 0) return Result.NO_RENDER;
        if (vertices == 0) return Result.NO_VERTICES;
        if (!Float.isFinite(maxAlpha) || maxAlpha <= .001F) return Result.TRANSPARENT;
        if (applies == 0) return Result.NO_SHADER_APPLY;
        if (programMismatch) return Result.WRONG_PROGRAM;
        if (changedPixels == 0) return Result.NO_PIXEL_CHANGE;
        return changedPixels > 0 ? Result.PIXELS_CHANGED : Result.DRAW_OBSERVED;
    }

    /** Compare RGB, ignoring alpha-only writes and one-byte color noise. Does not move buffers. */
    public static int changedPixels(ByteBuffer before, ByteBuffer after, int byteCount) {
        if (byteCount < 0 || byteCount % 4 != 0 || byteCount > before.limit() || byteCount > after.limit())
            throw new IllegalArgumentException("RGBA buffer sizes must match the requested region");
        int changed = 0;
        for (int offset = 0; offset < byteCount; offset += 4) {
            for (int channel = 0; channel < 3; channel++) {
                if (Math.abs(Byte.toUnsignedInt(before.get(offset + channel))
                        - Byte.toUnsignedInt(after.get(offset + channel))) > 1) {
                    changed++;
                    break;
                }
            }
        }
        return changed;
    }
}
