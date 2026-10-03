package com.nstut.celestialnail.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import static org.lwjgl.opengl.GL32C.*;

/** Isolated OpenGL compositor. Never replaces Minecraft/Iris shaders or their render targets.
 * Captures world depth before the hand/HUD clear, then composites after world post-processing.
 * All GL state touched here is restored directly, leaving the engine's state cache unchanged. */
public final class ProceduralCinematicPass {
    public record Frame(float flash, float dust, float age, float radius, float height,
                        float x, float y, float z, float atmosphere, int samples,
                        float pulseAge, int impactMode, float impactX, float impactY, float impactZ) {
        public boolean active() { return flash > .001F || dust > .001F || atmosphere > .001F; }
    }
    private static int program, vao, sourceFbo, outputFbo, color, depth, width, height;
    private static boolean failed, ready;
    private static final float[] inverse = new float[16];
    private static Frame frame;
    public static long draws;
    private ProceduralCinematicPass() {}

    public static void capture(int colorId, int depthId, int w, int h, Matrix4f inverseViewProjection, Frame next) {
        ready = false;
        if (failed || !next.active() || w <= 0 || h <= 0) return;
        try (State ignored = new State()) {
            initialize();
            allocate(w, h);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, sourceFbo);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorId, 0);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthId, 0);
            glReadBuffer(GL_COLOR_ATTACHMENT0);
            if (glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("World framebuffer is incomplete");
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, depth);
            if (next.dust > .001F) glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, 0, 0);
            checkError("capture");
            inverseViewProjection.get(inverse);
            frame = next;
            ready = true;
        } catch (RuntimeException ex) { disable(ex); }
    }

    public static void render(int mainColorId, int w, int h) {
        if (!ready || failed || w != width || h != height) return;
        ready = false;
        try (State ignored = new State()) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, sourceFbo);
            // Shader packs can change the main target attachment between capture and composition.
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mainColorId, 0);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, 0, 0);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, color);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, outputFbo);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mainColorId, 0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Composite framebuffer is incomplete");
            glViewport(0, 0, w, h);
            glDisable(GL_DEPTH_TEST); glDepthMask(false);
            glDisable(GL_BLEND); glDisable(GL_CULL_FACE); glDisable(GL_SCISSOR_TEST);
            glDisable(GL_FRAMEBUFFER_SRGB);
            glColorMask(true, true, true, true);
            glUseProgram(program);
            glBindVertexArray(vao);
            if (ignored.samplerObjects) {
                GL33C.glBindSampler(0, 0); GL33C.glBindSampler(1, 0);
            }
            glUniform1i(uniform("Scene"), 0); glUniform1i(uniform("Depth"), 1);
            glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, depth);
            glUniformMatrix4fv(uniform("InverseViewProjection"), false, inverse);
            glUniform2f(uniform("PixelSize"), 1F / w, 1F / h);
            glUniform3f(uniform("Center"), frame.x, frame.y, frame.z);
            glUniform4f(uniform("Event"), frame.age, frame.radius, frame.height, frame.dust);
            glUniform2f(uniform("Grade"), frame.flash, frame.atmosphere);
            var contact=new org.joml.Matrix4f().set(inverse).invert()
                    .transform(new org.joml.Vector4f(frame.impactX,frame.impactY,frame.impactZ,1));
            float focusX=.5F,focusY=.5F;
            if(contact.w>.001F) {
                focusX=Math.max(.08F,Math.min(.92F,contact.x/contact.w*.5F+.5F));
                focusY=Math.max(.08F,Math.min(.92F,contact.y/contact.w*.5F+.5F));
            }
            glUniform4f(uniform("Pulse"),frame.pulseAge,com.nstut.celestialnail.ImpactSequence.stage(frame.pulseAge,frame.impactMode),focusX,focusY);
            glUniform1i(uniform("Samples"), frame.samples);
            var shape=com.nstut.celestialnail.CinematicDustShape.at(frame.age,frame.radius,frame.height);
            glUniform4f(uniform("DustFront"),shape.front(),shape.width(),shape.curtainHeight(),shape.drop());
            glUniform4f(uniform("DustPlume"),shape.plumeRadius(),shape.rise(),shape.span(),shape.top());
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
            checkError("composite");
            draws++;
        } catch (RuntimeException ex) { disable(ex); }
    }

    private static void checkError(String operation) {
        int error=glGetError();
        if(error!=GL_NO_ERROR) throw new IllegalStateException(operation+" GL error: "+error);
    }
    private static int uniform(String name) { return glGetUniformLocation(program, name); }
    public static void invalidate() { ready = false; }
    private static void disable(RuntimeException ex) {
        failed = true; ready = false;
        System.getLogger("CelestialNail").log(System.Logger.Level.ERROR,
                "Procedural cinematics disabled after renderer failure; ordinary Nail rendering remains available", ex);
        close();
    }
    private static String source(String name) {
        try (var in = ProceduralCinematicPass.class.getResourceAsStream("/assets/celestial_nail/shaders/cinematic/" + name)) {
            if (in == null) throw new IOException("Missing cinematic shader " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    private static int shader(int type, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source(name)); glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id); glDeleteShader(id);
            throw new IllegalStateException(name + ": " + log);
        }
        return id;
    }
    private static void initialize() {
        if (program != 0) return;
        int vertex = 0, fragment = 0;
        try {
            vertex = shader(GL_VERTEX_SHADER, "cinematic.vsh");
            fragment = shader(GL_FRAGMENT_SHADER, "cinematic.fsh");
            program = glCreateProgram();
            glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
                throw new IllegalStateException(glGetProgramInfoLog(program));
            vao = glGenVertexArrays(); sourceFbo = glGenFramebuffers(); outputFbo = glGenFramebuffers();
        } finally {
            if (vertex != 0) glDeleteShader(vertex);
            if (fragment != 0) glDeleteShader(fragment);
        }
    }
    private static int texture(int format, int w, int h, int filtering) {
        int id = glGenTextures(); glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0,
                format == GL_RGBA8 ? GL_RGBA : GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filtering);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filtering);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return id;
    }
    private static void allocate(int w, int h) {
        if (width == w && height == h) return;
        if (color != 0) glDeleteTextures(color);
        if (depth != 0) glDeleteTextures(depth);
        glActiveTexture(GL_TEXTURE0);
        color = texture(GL_RGBA8, w, h, GL_LINEAR);
        depth = texture(GL_DEPTH_COMPONENT32F, w, h, GL_NEAREST);
        width = w; height = h;
    }
    public static void close() {
        ready = false;
        if (program != 0) glDeleteProgram(program);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (sourceFbo != 0) glDeleteFramebuffers(sourceFbo);
        if (outputFbo != 0) glDeleteFramebuffers(outputFbo);
        if (color != 0) glDeleteTextures(color);
        if (depth != 0) glDeleteTextures(depth);
        program = vao = sourceFbo = outputFbo = color = depth = width = height = 0;
    }

    private static final class State implements AutoCloseable {
        // Check before touching GL state. The 3.2 baseline has no sampler objects;
        // there the compositor's private textures provide their own filtering.
        final boolean samplerObjects = GL.getCapabilities().OpenGL33
                || GL.getCapabilities().GL_ARB_sampler_objects;
        final int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING), draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        final int shader = glGetInteger(GL_CURRENT_PROGRAM), vertex = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int unpack = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        final int active = glGetInteger(GL_ACTIVE_TEXTURE);
        final int[] viewport = new int[4], mask = new int[4];
        final int[] textures = new int[2], samplers = new int[2];
        final boolean depthTest = glIsEnabled(GL_DEPTH_TEST), depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        final boolean blend = glIsEnabled(GL_BLEND), cull = glIsEnabled(GL_CULL_FACE), scissor = glIsEnabled(GL_SCISSOR_TEST);
        final boolean srgb = glIsEnabled(GL_FRAMEBUFFER_SRGB);
        State() {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
            glGetIntegerv(GL_VIEWPORT, viewport); glGetIntegerv(GL_COLOR_WRITEMASK, mask);
            for (int i = 0; i < 2; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                textures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
                if (samplerObjects) samplers[i] = glGetInteger(GL33C.GL_SAMPLER_BINDING);
            }
            glActiveTexture(active);
        }
        public void close() {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, read); glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            glUseProgram(shader); glBindVertexArray(vertex);
            for (int i = 0; i < 2; i++) {
                glActiveTexture(GL_TEXTURE0 + i); glBindTexture(GL_TEXTURE_2D, textures[i]);
                if (samplerObjects) GL33C.glBindSampler(i, samplers[i]);
            }
            glActiveTexture(active);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, unpack);
            glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            glColorMask(mask[0] != 0, mask[1] != 0, mask[2] != 0, mask[3] != 0);
            enable(GL_DEPTH_TEST, depthTest); glDepthMask(depthWrite);
            enable(GL_BLEND, blend); enable(GL_CULL_FACE, cull); enable(GL_SCISSOR_TEST, scissor);
            enable(GL_FRAMEBUFFER_SRGB, srgb);
        }
        private static void enable(int capability, boolean enabled) { if (enabled) glEnable(capability); else glDisable(capability); }
    }
}
