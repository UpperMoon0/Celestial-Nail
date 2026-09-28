package com.nstut.celestialnail.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import com.nstut.celestialnail.CelestialNailVisuals;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class CelestialNailRenderer extends EntityRenderer<CelestialNailEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("celestial_nail", "textures/entity/celestial_nail.png");
    public CelestialNailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        CelestialNailEntity.clientVisualTick = CelestialNailPortalSound::tickEntity;
    }
    @Override
    public boolean shouldRender(CelestialNailEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.visualBounds());
    }
    @Override
    public void render(CelestialNailEntity nail, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        float age=nail.summonAge(partialTick), launchAge=nail.launchAge(partialTick), height=nail.nailHeight();
        float portalOffset=(float)(nail.portalY()-net.minecraft.util.Mth.lerp(partialTick,nail.yo,nail.getY()));
        boolean launched=nail.isLaunched(), impact=nail.isImpacting();
        int light=packedLight;
        float open=CelestialNailVisuals.opening(age, launchAge);
        float emergence=CelestialNailVisuals.emergence(age);
        if (!impact && age >= CelestialNailVisuals.OPEN_TICKS) {
            float unit=height/CelestialNailMesh.HEIGHT;
            float offset=CelestialNailVisuals.emergenceOffset(portalOffset,age);
            float bob=launched?0:(float)Math.sin(age*.045F)*height*.0025F*emergence;
            float ceiling=(portalOffset-offset-bob)/unit;
            pose.pushPose();
            pose.translate(0,offset+bob,0);
            pose.scale(unit,unit,unit);
            pose.mulPose(Axis.YP.rotationDegrees(launched?age*1.4F:age*.12F));
            draw(pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),CelestialNailMesh.BODY,light,ceiling,1.0F);
            pose.mulPose(Axis.YP.rotationDegrees(age*.8F));
            draw(pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),CelestialNailMesh.SHARDS,light,ceiling,1.0F);
            pose.popPose();
        }
        if (open > .001F) {
            pose.pushPose();
            pose.translate(0,portalOffset,0);
            float radius=height*.33F*open;
            pose.scale(radius,radius,radius);
            pose.mulPose(Axis.YP.rotationDegrees(age*.15F));
            draw(pose.last(),buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),CelestialNailMesh.PORTAL_CORE,light,Float.POSITIVE_INFINITY,1.0F);
            draw(pose.last(),buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),CelestialNailMesh.PORTAL_RIM,light,Float.POSITIVE_INFINITY,open);
            float haloAlpha=(.23F+.07F*(float)Math.sin(age*.09F))*open;
            draw(pose.last(),buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),CelestialNailMesh.PORTAL_HALO,light,Float.POSITIVE_INFINITY,haloAlpha);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-age*.55F));
            draw(pose.last(),buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),CelestialNailMesh.PORTAL_SPARKS,light,Float.POSITIVE_INFINITY,open);
            pose.popPose();
            float beamAlpha=(.55F+.12F*(float)Math.sin(age*.06F))*open;
            draw(pose.last(),buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),CelestialNailMesh.PORTAL_BEAM,light,Float.POSITIVE_INFINITY,beamAlpha);
            pose.popPose();
        }
        if (!impact && age >= CelestialNailVisuals.READY_TICKS) super.render(nail, entityYaw, partialTick, pose, buffers, packedLight);
    }
    private static void draw(PoseStack.Pose pose, VertexConsumer out, List<CelestialNailMesh.Face> faces, int packedLight, float ceiling, float alpha) {
        NailMeshClipper.emit(faces,ceiling,(face,point,u,v) -> vertex(pose,out,face,point,u,v,
                face.emissive()?LightTexture.FULL_BRIGHT:packedLight,alpha));
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer out, CelestialNailMesh.Face face,
                               CelestialNailMesh.Point point, float s, float t, int light, float alpha) {
        float u = (face.material() * 16 + .5F + s * 15) / 128.0F;
        float v = (.5F + t * 15) / 16.0F;
        float shade = face.shade();
        out.vertex(pose.pose(), point.x(), point.y(), point.z()).color(shade, shade, shade, alpha)
                    .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.normal(), face.normal().x(), face.normal().y(), face.normal().z()).endVertex();
    }
    @Override
    public ResourceLocation getTextureLocation(CelestialNailEntity entity) { return TEXTURE; }
}
