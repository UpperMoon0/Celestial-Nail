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
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("celestial_nail", "textures/entity/celestial_nail.png");
    public CelestialNailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        CelestialNailEntity.clientVisualTick = CelestialNailAtmosphere::track;
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
        float visibility=CelestialNailAtmosphere.visibility(nail.getX(),nail.getZ()), impactAge=nail.impactAge(partialTick), crumbleAge=nail.crumbleAge(partialTick), impactOffset=(float)(nail.impactOrigin().y-net.minecraft.util.Mth.lerp(partialTick,nail.yo,nail.getY()));
        float open=CelestialNailVisuals.opening(age, launchAge)*(crumbleAge<0?1:1-CelestialNailVisuals.smooth(crumbleAge/15));
        float emergence=CelestialNailVisuals.emergence(age);
        if (age >= CelestialNailVisuals.OPEN_TICKS) {
            float unit=height/CelestialNailMesh.HEIGHT;
            float offset=impactAge>=0?0:CelestialNailVisuals.emergenceOffset(portalOffset,age-Math.max(0,crumbleAge));
            float bob=launched?0:(float)Math.sin(age*.045F)*height*.0025F*emergence;
            float ceiling=(portalOffset-offset-bob)/unit;
            pose.pushPose();
            pose.translate(0,offset+bob,0);
            pose.scale(unit,unit,unit);
            pose.mulPose(Axis.YP.rotationDegrees(launchAge>=0?(age-Math.max(0,impactAge)-Math.max(0,crumbleAge))*1.4F:(age-Math.max(0,crumbleAge))*.12F));
            drawBody(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),light,ceiling,visibility,crumbleAge);
            if(crumbleAge<0 && impactAge<0) {
                float phase=launchAge>=0?Math.max(0,1-launchAge/24):1-(age%100)/100;
                drawPulse(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),light,ceiling,visibility,phase);
            }
            pose.mulPose(Axis.YP.rotationDegrees(age*.8F));
            draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.SHARDS,light,ceiling,visibility);
            pose.popPose();
        }

        if(!impact && crumbleAge<0 && age>=CelestialNailVisuals.READY_TICKS) {
            float motion=launchAge>=0 ? age-launchAge : age;
            pose.pushPose();pose.scale(height,height,height);
            pose.mulPose(Axis.YP.rotationDegrees(motion*.18F));
            draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.DEBRIS,light,Float.POSITIVE_INFINITY,visibility);
            pose.popPose();

        }
        if(impact && crumbleAge<0 && impactAge>=0) {
            float columnAlpha=Math.max(0,1-impactAge/28)*visibility;
            if(columnAlpha>.001F) {
                pose.pushPose();pose.translate(0,impactOffset,0);pose.scale(height,height*5,height);
                draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.IMPACT_COLUMN,light,Float.POSITIVE_INFINITY,columnAlpha);
                pose.popPose();
            }
            float radius=com.nstut.celestialnail.CataclysmTimeline.shockRadius(impactAge);
            float ringAlpha=Math.max(0,1-impactAge/70)*visibility;
            if(radius>0 && ringAlpha>.001F) {
                pose.pushPose();pose.translate(0,impactOffset+1,0);pose.scale(radius/.9F,1,radius/.9F);
                draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.SHOCK_SURFACE,light,Float.POSITIVE_INFINITY,ringAlpha);
                pose.popPose();
            }
        }
        if (open > .001F) {
            pose.pushPose();
            pose.translate(0,portalOffset,0);
            float radius=height*.33F*open;
            pose.scale(radius,radius,radius);
            pose.mulPose(Axis.YP.rotationDegrees(age*.15F));
            draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.PORTAL_CORE,light,Float.POSITIVE_INFINITY,visibility);
            draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.PORTAL_RIM,light,Float.POSITIVE_INFINITY,open*visibility);
            float haloAlpha=(.23F+.07F*(float)Math.sin(age*.09F))*open;
            draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.PORTAL_HALO,light,Float.POSITIVE_INFINITY,haloAlpha*visibility);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-age*.55F));
            draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.PORTAL_SPARKS,light,Float.POSITIVE_INFINITY,open*visibility);
            pose.popPose();
            float beamAlpha=(.55F+.12F*(float)Math.sin(age*.06F))*open;
            draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.PORTAL_BEAM,light,Float.POSITIVE_INFINITY,beamAlpha*visibility);
            pose.popPose();
        }
        if (!impact && age >= CelestialNailVisuals.READY_TICKS) super.render(nail, entityYaw, partialTick, pose, buffers, packedLight);
    }
    private static CelestialNailMesh.Face effectFace(CelestialNailMesh.Face f,int material) {
        return new CelestialNailMesh.Face(f.a(),f.b(),f.c(),f.d(),f.normal(),material,1);
    }
    private static void drawPulse(PoseStack.Pose pose,VertexConsumer out,int light,float ceiling,float alpha,float phase) {
        NailMeshClipper.emit(CelestialNailMesh.BODY,ceiling,(f,p,u,v)->{
            if(!f.emissive())return;
            var offset=new CelestialNailMesh.Point(p.x()+f.normal().x()*.002F,p.y()+f.normal().y()*.002F,p.z()+f.normal().z()*.002F);
            vertex(pose,out,effectFace(f,9),offset,p.y()/CelestialNailMesh.HEIGHT,phase,light,alpha);
        });
    }
    private static void drawBody(PoseStack.Pose pose,VertexConsumer out,int light,float ceiling,float alpha,float crumble) {
        if(crumble<0){draw(pose,out,CelestialNailMesh.BODY,light,ceiling,alpha);return;}
        NailMeshClipper.emit(CelestialNailMesh.BODY,ceiling,(f,p,u,v)->{
            float cy=(f.a().y()+f.b().y()+f.c().y()+f.d().y())*.25F;
            float cx=(f.a().x()+f.b().x()+f.c().x()+f.d().x())*.25F;
            float cz=(f.a().z()+f.b().z()+f.c().z()+f.d().z())*.25F;
            double sector=Math.floor((Math.atan2(cz,cx)+Math.PI)*4/Math.PI);
            double seed=Math.sin(Math.floor(cy*2)*73+sector*37)*437.53;
            float random=(float)(seed-Math.floor(seed));
            float fall=Math.max(0,crumble-16-random*24);
            float fade=1-CelestialNailVisuals.smooth((crumble-48)/32);
            float angle=fall*.018F*(random-.5F),cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
            var part=new CelestialNailMesh.Point(p.x()*cos-p.z()*sin+cx*fall*.025F,p.y()-fall*fall*.0022F,p.x()*sin+p.z()*cos+cz*fall*.025F);
            vertex(pose,out,f,part,u,v,light,alpha*fade);

        });
        if(crumble<16)NailMeshClipper.emit(CelestialNailMesh.BODY,ceiling,(f,p,u,v)->{
            var crack=new CelestialNailMesh.Point(p.x()+f.normal().x()*.003F,p.y()+f.normal().y()*.003F,p.z()+f.normal().z()*.003F);
            vertex(pose,out,effectFace(f,10),crack,u,crumble/16,light,alpha);
        });
    }
    private static void draw(PoseStack.Pose pose, VertexConsumer out, List<CelestialNailMesh.Face> faces, int packedLight, float ceiling, float alpha) {
        NailMeshClipper.emit(faces,ceiling,(face,point,u,v) -> vertex(pose,out,face,point,u,v,
                face.emissive()?LightTexture.FULL_BRIGHT:packedLight,alpha));
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer out, CelestialNailMesh.Face face,
                               CelestialNailMesh.Point point, float s, float t, int light, float alpha) {
        float u = (face.material() * 16 + .5F + s * 15) / 128.0F;
        float v = (.5F + t * 15) / 16.0F;
        if(face.material()==8){u=-3+s;v=t;}
        if(face.material()==9){u=-5+s;v=t;}
        if(face.material()==10){u=-7+s;v=t;}
        float shade = face.shade() * (face.emissive()?1:.45F+.55F*Math.max((light>>4)&15,(light>>20)&15)/15F);
        out.addVertex(pose, point.x(), point.y(), point.z()).setColor(shade, shade, shade, alpha)
                    .setUv(u, v);
    }
    @Override
    public ResourceLocation getTextureLocation(CelestialNailEntity entity) { return TEXTURE; }
}
