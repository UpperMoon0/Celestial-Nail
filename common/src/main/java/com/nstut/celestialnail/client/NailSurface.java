package com.nstut.celestialnail.client;

import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.client.CelestialNailMesh.Face;
import com.nstut.celestialnail.client.CelestialNailMesh.Point;
import java.util.List;

/** Ordinary atlas UVs and tint, independent of whichever shader the client installs. */
public final class NailSurface {
    public record Sample(float u,float v,float red,float green,float blue,float alpha) {}
    public static final List<Face> CRYSTALS=CelestialNailMesh.BODY.stream()
            .filter(f->f.material()==3 || f.material()==4 || f.material()==6).toList();
    private NailSurface() {}

    public static Sample sample(Face face,Point point,float s,float t,float alpha,float age) {
        int material=face.material();
        int tile=material>=8?6:material;
        float u=(tile*16+.5F+s*15)/128F, v=(.5F+t*15)/16F;
        float red=1,green=1,blue=1;
        if(age>=0 && (material==3 || material==4 || material==6)) {
            // A restrained per-entity glint; RGB remains tint, never an encoded animation clock.
            float phase=(age%960F)/960F*(float)(Math.PI*2);
            float seed=seed(face);
            float flow=(float)Math.sin(t*9+Math.sin(s*7+phase)*.65-phase*2+seed*6.28);
            float glint=(float)Math.pow(.5+.5*Math.sin(phase*3+seed*31),8);
            float shade=.88F+.07F*flow+.05F*glint;
            red=green=blue=shade;
        } else if(material==8) {
            float radius=(float)Math.hypot(point.x(),point.z());
            float edge=(float)Math.exp(-Math.pow((radius-.90)/.016,2));
            alpha*=.08F+.48F*edge;
            red=.35F+.43F*edge;green=.75F+.21F*edge;blue=1;
        } else if(material==9) {
            float distance=point.y()/CelestialNailMesh.HEIGHT-age+(seed(face)-.5F)*.075F;
            float front=(float)Math.exp(-Math.pow(distance/.023F,2));
            float wake=(float)Math.exp(-Math.max(distance,0)/.095F)*CelestialNailVisuals.smooth(distance/.028F);
            float envelope=CelestialNailVisuals.smooth(age/.06F)*(1-CelestialNailVisuals.smooth((age-.94F)/.06F));
            alpha*=Math.min(.48F,(front*.35F+wake*.10F)*envelope);
            red=.10F+.38F*front;green=.42F+.49F*front;blue=1;
        }
        return new Sample(u,v,red,green,blue,Math.max(0,Math.min(1,alpha)));
    }

    private static float seed(Face face) {
        int bits=Float.floatToIntBits(face.a().x()*17+face.a().y()*31+face.a().z()*47)&0x7fffffff;
        return (bits%251)/250F;
    }
}
