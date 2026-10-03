package com.nstut.celestialnail.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Version adapter: capture real world projection/depth before hand rendering clears it. */
public final class CelestialNailCinematics {
    private CelestialNailCinematics() {}
    public static void capture(float partial) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null) {ProceduralCinematicPass.invalidate();return;}
        var camera=mc.gameRenderer.getMainCamera();
        Vec3 eye=camera.getPosition();
        Vector3f look=new Vector3f(0,0,-1).rotate(camera.rotation());
        var frame=CelestialNailAtmosphere.cinematic(partial,eye,new Vec3(look.x,look.y,look.z));
        Matrix4f inverse=new Matrix4f(RenderSystem.getProjectionMatrix())
                .mul(new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()))).invert();
        var target=mc.getMainRenderTarget();
        ProceduralCinematicPass.capture(target.getColorTextureId(),target.getDepthTextureId(),target.width,target.height,inverse,frame);
    }
    public static void render() {
        var target=Minecraft.getInstance().getMainRenderTarget();
        ProceduralCinematicPass.render(target.getColorTextureId(),target.width,target.height);
    }
}
