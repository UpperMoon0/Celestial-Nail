package com.nstut.celestialnail.client;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import java.nio.ByteBuffer;

/** Opt-in synchronous GPU readback, at most one custom draw per diagnostic interval. */
final class NailDrawPixelProbe {
    private static ByteBuffer before, after;
    private static int x, y, width, height, framebuffer, drawBuffer;
    private static boolean pending;

    private NailDrawPixelProbe() {}
    static void begin() {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        width = Math.min(2048, viewport[2]);
        height = Math.min(2048, viewport[3]);
        if (width <= 0 || height <= 0) return;
        x = viewport[0] + (viewport[2] - width) / 2;
        y = viewport[1] + (viewport[3] - height) / 2;
        framebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        drawBuffer = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_DRAW_BUFFER0);
        if (drawBuffer == GL11.GL_NONE) return;
        int bytes = width * height * 4;
        if (before == null || before.capacity() < bytes) {
            before = BufferUtils.createByteBuffer(bytes);
            after = BufferUtils.createByteBuffer(bytes);
        }
        read(before);
        pending = true;
    }
    static int finish() {
        if (!pending) return -1;
        pending = false;
        if (framebuffer != GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)) return -1;
        read(after);
        return RenderDebugChecks.changedPixels(before, after, width * height * 4);
    }
    static void cancel() { pending = false; }

    private static void read(ByteBuffer target) {
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int readBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int alignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        int rowLength = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS);
        int targetReadBuffer = -1;
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer);
            targetReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
            GL11.glReadBuffer(drawBuffer);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
            target.clear().limit(width * height * 4);
            GL11.glReadPixels(x, y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, target);
        } finally {
            if (targetReadBuffer != -1) GL11.glReadBuffer(targetReadBuffer);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, skipPixels);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL11.glReadBuffer(readBuffer);
        }
    }
}
