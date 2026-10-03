package com.nstut.celestialnail.client;

import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Render-stage adapter for the extracted camera state and OpenGL GPU textures. */
public final class CelestialNailCinematics {
    private CelestialNailCinematics() {}
    public static void capture(float partial, Matrix4f projection) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || projection==null) {ProceduralCinematicPass.invalidate();return;}
        var camera=mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Vector3f look=new Vector3f(0,0,-1).rotate(camera.orientation);
        var frame=CelestialNailAtmosphere.cinematic(partial,camera.pos,new Vec3(look.x,look.y,look.z));
        var target=mc.getMainRenderTarget();
        if(!(target.getColorTexture() instanceof GlTexture color) || !(target.getDepthTexture() instanceof GlTexture depth)) {
            ProceduralCinematicPass.invalidate(); return;
        }
        Matrix4f inverse=new Matrix4f(projection).mul(camera.viewRotationMatrix).invert();
        ProceduralCinematicPass.capture(color.glId(),depth.glId(),target.width,target.height,inverse,frame);
    }
    public static void render() {
        var target=Minecraft.getInstance().getMainRenderTarget();
        if(target.getColorTexture() instanceof GlTexture color)
            ProceduralCinematicPass.render(color.glId(),target.width,target.height);
    }
}
