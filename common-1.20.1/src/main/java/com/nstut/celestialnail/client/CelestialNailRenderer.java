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
        float bodyImpactAge=impactAge<0?-1:Math.max(0,impactAge-Math.max(0,crumbleAge));
        float open=CelestialNailVisuals.opening(age, launchAge)*(crumbleAge<0?1:1-CelestialNailVisuals.smooth(crumbleAge/15));
        float emergence=CelestialNailVisuals.emergence(age);
        if (age >= CelestialNailVisuals.OPEN_TICKS) {
            float unit=height/CelestialNailMesh.HEIGHT;
            float offset=impactAge>=0?impactOffset-com.nstut.celestialnail.CataclysmTimeline.pierceDepth(nail.power(),height,bodyImpactAge):CelestialNailVisuals.emergenceOffset(portalOffset,age-Math.max(0,crumbleAge));
            float bob=launched?0:(float)Math.sin(age*.045F)*height*.0025F*emergence;
            float ceiling=(portalOffset-offset-bob)/unit;
            pose.pushPose();
            pose.translate(0,offset+bob,0);
            pose.scale(unit,unit,unit);
            pose.mulPose(Axis.YP.rotationDegrees(launchAge>=0?(age-Math.max(0,bodyImpactAge)-Math.max(0,crumbleAge))*1.4F:(age-Math.max(0,crumbleAge))*.12F));
            drawBody(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),light,ceiling,visibility,crumbleAge,age);
            if(crumbleAge<0 && impactAge<0) {
                float phase=launchAge>=0?Math.max(0,1-launchAge/24):1-(age%100)/100;
                drawPulse(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),light,ceiling,visibility,phase);
            }
            pose.mulPose(Axis.YP.rotationDegrees((age-Math.max(0,crumbleAge))*.8F));
            if(crumbleAge<0) draw(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailMesh.SHARDS,light,ceiling,visibility,age);
            else drawFragments(pose.last(),buffers.getBuffer(CelestialNailSkyRender.TYPE),CelestialNailFracture.SHARDS,light,ceiling,visibility,crumbleAge,age);
            pose.popPose();
        }

        if(!impact && crumbleAge<0 && age>=CelestialNailVisuals.READY_TICKS) {
            // Keep the orbit clock continuous through charge, portal closure and descent.
            float motion=age;
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
            vertex(pose,out,effectFace(f,9),offset,u,v,light,alpha,phase);
        });
    }
    private static void drawBody(PoseStack.Pose pose,VertexConsumer out,int light,float ceiling,float alpha,float crumble,float age) {
        if(crumble<0){draw(pose,out,CelestialNailMesh.BODY,light,ceiling,alpha,age);return;}
        drawFragments(pose,out,CelestialNailFracture.BODY,light,ceiling,alpha,crumble,age);
    }
    private static void drawFragments(PoseStack.Pose pose,VertexConsumer out,List<CelestialNailFracture.Piece> pieces,
                                      int light,float ceiling,float alpha,float crumble,float age) {
        for(var piece:pieces) {
            var motion=CelestialNailFracture.motion(piece,crumble);
            if(motion.alpha()<.002F)continue;
            NailMeshClipper.emit(piece.faces(),ceiling,(f,p,u,v)->
                    vertex(pose,out,f,motion.apply(p),u,v,light,alpha*motion.alpha(),age));
        }
    }
    private static void draw(PoseStack.Pose pose, VertexConsumer out, List<CelestialNailMesh.Face> faces, int packedLight, float ceiling, float alpha) {
        draw(pose,out,faces,packedLight,ceiling,alpha,-1);
    }
    private static void draw(PoseStack.Pose pose, VertexConsumer out, List<CelestialNailMesh.Face> faces, int packedLight, float ceiling, float alpha, float age) {
        NailMeshClipper.emit(faces,ceiling,(face,point,u,v) -> vertex(pose,out,face,point,u,v,
                face.emissive()?LightTexture.FULL_BRIGHT:packedLight,alpha,age));
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer out, CelestialNailMesh.Face face,
                               CelestialNailMesh.Point point, float s, float t, int light, float alpha) {
        vertex(pose,out,face,point,s,t,light,alpha,-1);
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer out, CelestialNailMesh.Face face,
                               CelestialNailMesh.Point point, float s, float t, int light, float alpha, float age) {
        float u = (face.material() * 16 + .5F + s * 15) / 128.0F;
        float v = (.5F + t * 15) / 16.0F;
        if(face.material()==8){u=-3+s;v=t;}
        if(face.material()==9){u=-5+s;v=t;}
        float shade = face.shade() * (face.emissive()?1:.45F+.55F*Math.max((light>>4)&15,(light>>20)&15)/15F);
        float green=shade, blue=shade;
        if(age>=0 && (face.material()==3 || face.material()==4 || face.material()==6)) {
            // Positive UVs outside the atlas opt only nail crystals into the glint shader.
            // Two color bytes carry a smooth per-entity clock, safe for deferred/batched draws.
            int clock=Math.round((age%960F)/960F*65535F);
            green=(clock>>>8)/255F; blue=(clock&255)/255F;
            u=2+face.material()+s*.9F;
            int seed=(Float.floatToIntBits(face.a().x()*17+face.a().y()*31+face.a().z()*47)&0x7fffffff)%251;
            v=seed+t*.9F;
        }
        if(face.material()==9) {
            // Pulse: precise longitudinal height/phase in UV; facet UV and seed in RGB.
            // It follows the glass instead of forming a uniformly lit band around the nail.
            u=-5+point.y()/CelestialNailMesh.HEIGHT; v=age;
            shade=s; green=t;
            int seed=(Float.floatToIntBits(face.a().x()*17+face.a().y()*31+face.a().z()*47)&0x7fffffff)%251;
            blue=seed/255F;
        }
        out.vertex(pose.pose(), point.x(), point.y(), point.z()).color(shade, green, blue, alpha)
                    .uv(u, v).endVertex();
    }
    @Override
    public ResourceLocation getTextureLocation(CelestialNailEntity entity) { return TEXTURE; }
}
