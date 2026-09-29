package com.nstut.celestialnail.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import com.nstut.celestialnail.CelestialNailVisuals;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import java.util.List;

public final class CelestialNailRenderer extends EntityRenderer<CelestialNailEntity, CelestialNailRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("celestial_nail", "textures/entity/celestial_nail.png");
    public CelestialNailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        CelestialNailEntity.clientVisualTick = CelestialNailAtmosphere::track;
    }
    @Override
    protected AABB getBoundingBoxForCulling(CelestialNailEntity entity) {
        return entity.visualBounds();
    }
    @Override
    public CelestialNailRenderState createRenderState() { return new CelestialNailRenderState(); }
    @Override
    public void extractRenderState(CelestialNailEntity entity, CelestialNailRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.launched = entity.isLaunched();
        state.height=entity.nailHeight();
        state.summonAge=entity.summonAge(partialTick);
        state.launchAge=entity.launchAge(partialTick);
        state.portalOffset=(float)(entity.portalY()-net.minecraft.util.Mth.lerp(partialTick,entity.yo,entity.getY()));
        state.impact=entity.isImpacting();
        state.visibility=CelestialNailAtmosphere.visibility(entity.getX(),entity.getZ());
        state.impactAge=entity.impactAge(partialTick);
    }
    @Override
    public void submit(CelestialNailRenderState state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera) {
        float age=state.summonAge, launchAge=state.launchAge, height=state.height, portalOffset=state.portalOffset;
        boolean launched=state.launched, impact=state.impact;
        int light=state.lightCoords;
        float visibility=state.visibility, impactAge=state.impactAge;
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
            nodes.submitCustomGeometry(pose, CelestialNailSkyRender.TYPE, (transform, out) -> draw(transform,out,CelestialNailMesh.BODY,light,ceiling,visibility));
            pose.mulPose(Axis.YP.rotationDegrees(age*.8F));
            nodes.submitCustomGeometry(pose, CelestialNailSkyRender.TYPE, (transform, out) -> draw(transform,out,CelestialNailMesh.SHARDS,light,ceiling,visibility));
            pose.popPose();
        }

        if(!impact && age>=CelestialNailVisuals.READY_TICKS) {
            float motion=launchAge>=0 ? age-launchAge : age;
            pose.pushPose();pose.scale(height,height,height);
            pose.mulPose(Axis.YP.rotationDegrees(motion*.18F));
            nodes.submitCustomGeometry(pose,CelestialNailSkyRender.TYPE,(transform,out)->draw(transform,out,CelestialNailMesh.DEBRIS,light,Float.POSITIVE_INFINITY,visibility));
            pose.popPose();
            float pulse=launchAge>=0 ? Math.max(0,1-launchAge/24) : 1-(age%100)/100;
            pose.pushPose();pose.translate(0,height*pulse,0);pose.scale(height,height,height);
            nodes.submitCustomGeometry(pose,CelestialNailSkyRender.TYPE,(transform,out)->draw(transform,out,CelestialNailMesh.PULSE_RING,light,Float.POSITIVE_INFINITY,visibility*.65F));
            pose.popPose();
        }
        if(impact && impactAge>=0) {
            float columnAlpha=Math.max(0,1-impactAge/28)*visibility;
            if(columnAlpha>.001F) {
                pose.pushPose();pose.scale(height,height*5,height);
                nodes.submitCustomGeometry(pose,CelestialNailSkyRender.TYPE,(transform,out)->draw(transform,out,CelestialNailMesh.IMPACT_COLUMN,light,Float.POSITIVE_INFINITY,columnAlpha));
                pose.popPose();
            }
            float radius=com.nstut.celestialnail.CataclysmTimeline.shockRadius(impactAge);
            float ringAlpha=Math.max(0,1-impactAge/70)*visibility;
            if(radius>0 && ringAlpha>.001F) {
                pose.pushPose();pose.translate(0,1,0);pose.scale(radius,Math.max(3,radius*.1F),radius);
                nodes.submitCustomGeometry(pose,CelestialNailSkyRender.TYPE,(transform,out)->draw(transform,out,CelestialNailMesh.SHOCK_RING,light,Float.POSITIVE_INFINITY,ringAlpha));
                pose.popPose();
            }
        }
        if (open > .001F) {
            pose.pushPose();
            pose.translate(0,portalOffset,0);
            float radius=height*.33F*open;
            pose.scale(radius,radius,radius);
            pose.mulPose(Axis.YP.rotationDegrees(age*.15F));
            nodes.submitCustomGeometry(pose, CelestialNailSkyRender.TYPE, (transform, out) -> draw(transform,out,CelestialNailMesh.PORTAL_CORE,light,Float.POSITIVE_INFINITY,visibility));
            nodes.submitCustomGeometry(pose, CelestialNailSkyRender.TYPE, (transform, out) -> draw(transform,out,CelestialNailMesh.PORTAL_RIM,light,Float.POSITIVE_INFINITY,open*visibility));
            float haloAlpha=(.23F+.07F*(float)Math.sin(age*.09F))*open;
            nodes.submitCustomGeometry(pose, CelestialNailSkyRender.TYPE, (transform, out) -> draw(transform,out,CelestialNailMesh.PORTAL_HALO,light,Float.POSITIVE_INFINITY,haloAlpha*visibility));
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-age*.55F));
            nodes.submitCustomGeometry(pose, CelestialNailSkyRender.TYPE, (transform, out) -> draw(transform,out,CelestialNailMesh.PORTAL_SPARKS,light,Float.POSITIVE_INFINITY,open*visibility));
            pose.popPose();
            float beamAlpha=(.55F+.12F*(float)Math.sin(age*.06F))*open;
            nodes.submitCustomGeometry(pose, CelestialNailSkyRender.TYPE, (transform, out) -> draw(transform,out,CelestialNailMesh.PORTAL_BEAM,light,Float.POSITIVE_INFINITY,beamAlpha*visibility));
            pose.popPose();
        }
        if (!impact && age >= CelestialNailVisuals.READY_TICKS) super.submit(state, pose, nodes, camera);
    }
    private static void draw(PoseStack.Pose pose, VertexConsumer out, List<CelestialNailMesh.Face> faces, int packedLight, float ceiling, float alpha) {
        NailMeshClipper.emit(faces,ceiling,(face,point,u,v) -> vertex(pose,out,face,point,u,v,
                face.emissive()?LightCoordsUtil.FULL_BRIGHT:packedLight,alpha));
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer out, CelestialNailMesh.Face face,
                               CelestialNailMesh.Point point, float s, float t, int light, float alpha) {
        float u = (face.material() * 16 + .5F + s * 15) / 128.0F;
        float v = (.5F + t * 15) / 16.0F;
        float shade = face.shade() * (face.emissive()?1:.45F+.55F*Math.max((light>>4)&15,(light>>20)&15)/15F);
        out.addVertex(pose, point.x(), point.y(), point.z()).setColor(shade, shade, shade, alpha)
                    .setUv(u, v);
    }
}
