package com.nstut.celestialnail.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Method;

/** Identifies Nail draws by their unique texture, including Oculus's replacement shaders. */
public final class NailDrawContext {
    private static final ResourceLocation TEXTURE = new ResourceLocation("celestial_nail", "textures/entity/celestial_nail.png");
    private static final Object API;
    private static final Method SHADOW;
    static {
        Object api = null;
        Method shadow = null;
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = type.getMethod("getInstance").invoke(null);
            shadow = type.getMethod("isRenderingShadowPass");
        } catch (ClassNotFoundException ignored) {
            // Optional Oculus integration.
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot initialize Nail draw diagnostics", ex);
        }
        API = api; SHADOW = shadow;
    }
    private NailDrawContext() {}
    public static boolean mainNailDraw() {
        if (!CelestialNailRenderDebug.isEnabled() && !RenderRegressionHooks.ACTIVE) return false;
        if (RenderSystem.getShaderTexture(0) != Minecraft.getInstance().getTextureManager().getTexture(TEXTURE).getId()) return false;
        if (API == null) return true;
        try { return !(boolean) SHADOW.invoke(API); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException("Cannot query shader shadow pass", ex); }
    }
}
