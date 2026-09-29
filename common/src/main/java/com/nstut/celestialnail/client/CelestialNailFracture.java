package com.nstut.celestialnail.client;

import com.nstut.celestialnail.CelestialNailVisuals;
import java.util.*;
import static com.nstut.celestialnail.client.CelestialNailMesh.*;

/** Baked, thick fracture shells; all faces in a fragment share one rigid transform. */
public final class CelestialNailFracture {
    public record Piece(List<Face> faces, Point pivot, float seed) {}
    public record Motion(Point pivot,float cosY,float sinY,float cosZ,float sinZ,float dx,float dy,float dz,float alpha) {
        public Point apply(Point p) {
            float x=p.x()-pivot.x(), y=p.y()-pivot.y(), z=p.z()-pivot.z();
            float rx=x*cosY-z*sinY, rz=x*sinY+z*cosY;
            return new Point(pivot.x()+rx*cosZ-y*sinZ+dx,pivot.y()+rx*sinZ+y*cosZ+dy,pivot.z()+rz+dz);
        }
    }
    public static final List<Piece> BODY=bake(CelestialNailMesh.BODY);
    public static final List<Piece> SHARDS=bake(CelestialNailMesh.SHARDS);
    private CelestialNailFracture() {}
    private static List<Piece> bake(List<Face> source) {
        Map<Integer,List<Face>> groups=new LinkedHashMap<>();
        for(Face original:source) for(Face f:split(original)) {
            float x=(f.a().x()+f.b().x()+f.c().x()+f.d().x())*.25F;
            float y=(f.a().y()+f.b().y()+f.c().y()+f.d().y())*.25F;
            float z=(f.a().z()+f.b().z()+f.c().z()+f.d().z())*.25F;
            int sector=(int)Math.floor((Math.atan2(z,x)+Math.PI)*4/Math.PI);
            int key=(int)Math.floor(y/.48F)*8+Math.min(7,sector);
            var faces=groups.computeIfAbsent(key,k->new ArrayList<>());faces.add(f);
            // Give large exposed surfaces thickness without multiplying tiny trim geometry.
            Point cross=f.b().sub(f.a()).cross(f.c().sub(f.a()));
            if(cross.x()*cross.x()+cross.y()*cross.y()+cross.z()*cross.z()<.000025F)continue;
            // Some baked quads have reversed winding (the renderer is deliberately two-sided).
            // Extrude toward the shaft axis rather than accidentally covering the ivory exterior.
            float alignment=f.normal().x()*x+f.normal().z()*z;
            if(Math.abs(alignment)<.001F)alignment=f.normal().y()*(y>CelestialNailMesh.HEIGHT*.5F?1:-1);
            Point inward=f.normal().mul(alignment>=0?-.045:.045);
            Point a=f.a().add(inward),b=f.b().add(inward),c=f.c().add(inward),d=f.d().add(inward);
            int mat=f.emissive()?3:2;
            faces.add(new Face(d,c,b,a,f.normal().mul(-1),mat,.56F));
            Point[] outer={f.a(),f.b(),f.c(),f.d()},inner={a,b,c,d};
            for(int i=0;i<4;i++) {
                int j=(i+1)%4;
                Point n=outer[j].sub(outer[i]).cross(inner[i].sub(outer[i]));
                if(n.x()*n.x()+n.y()*n.y()+n.z()*n.z()<1e-12)continue;
                faces.add(new Face(outer[i],outer[j],inner[j],inner[i],n.unit(),mat,.68F));
            }
        }
        List<Piece> pieces=new ArrayList<>();
        groups.forEach((key,faces)->{
            float x=0,y=0,z=0;
            for(Face f:faces){x+=f.a().x();y+=f.a().y();z+=f.a().z();}
            double noise=Math.sin(key*78.233+3.17)*43758.5453;
            pieces.add(new Piece(List.copyOf(faces),new Point(x/faces.size(),y/faces.size(),z/faces.size()),(float)(noise-Math.floor(noise))));
        });
        return List.copyOf(pieces);
    }
    private static List<Face> split(Face face) {
        float low=Math.min(Math.min(face.a().y(),face.b().y()),Math.min(face.c().y(),face.d().y()));
        float high=Math.max(Math.max(face.a().y(),face.b().y()),Math.max(face.c().y(),face.d().y()));
        if(high-low<.48F)return List.of(face);
        List<Face> result=new ArrayList<>();
        for(int band=(int)Math.floor(low/.48F);band<=(int)Math.floor(high/.48F);band++) {
            List<Point> polygon=clip(List.of(face.a(),face.b(),face.c(),face.d()),band*.48F,true);
            polygon=clip(polygon,(band+1)*.48F,false);
            for(int i=1;i+1<polygon.size();i++) {
                Point a=polygon.get(0),b=polygon.get(i),c=polygon.get(i+1);
                Point cross=b.sub(a).cross(c.sub(a));
                if(cross.x()*cross.x()+cross.y()*cross.y()+cross.z()*cross.z()>1e-12)
                    result.add(new Face(a,b,c,c,face.normal(),face.material(),face.shade()));
            }
        }
        return result;
    }
    private static List<Point> clip(List<Point> polygon,float height,boolean above) {
        List<Point> result=new ArrayList<>();if(polygon.isEmpty())return result;
        Point last=polygon.get(polygon.size()-1);boolean previous=above?last.y()>=height:last.y()<=height;
        for(Point current:polygon) {
            boolean inside=above?current.y()>=height:current.y()<=height;
            if(inside!=previous) {
                float t=(height-last.y())/(current.y()-last.y());
                result.add(new Point(last.x()+(current.x()-last.x())*t,height,last.z()+(current.z()-last.z())*t));
            }
            if(inside)result.add(current);last=current;previous=inside;
        }
        return result;
    }
    public static Motion motion(Piece piece,float age) {
        Point p=piece.pivot();float seed=piece.seed();
        float release=6+p.y()/CelestialNailMesh.HEIGHT*9+seed*4;
        // Open actual fragment boundaries before release, instead of painting cracks on each face.
        float strain=CelestialNailVisuals.smooth((age-(release-6))/6);
        float separation=strain*(.008F+seed*.009F);
        float fall=Math.max(0,age-release);
        float impulse=CelestialNailVisuals.smooth(fall/4);
        float yaw=(strain*.008F+fall*.065F)*(seed-.5F),roll=(strain*.004F+fall*(.018F+seed*.023F))*(seed>.5F?1:-1);
        float length=(float)Math.hypot(p.x(),p.z());
        float burst=separation+(.045F*impulse+fall*.009F)*( .65F+seed*.7F);
        float alpha=1-CelestialNailVisuals.smooth((fall-18)/17);
        return new Motion(p,(float)Math.cos(yaw),(float)Math.sin(yaw),(float)Math.cos(roll),(float)Math.sin(roll),
                length<.001F?0:p.x()/length*burst,-.012F*fall*fall,length<.001F?0:p.z()/length*burst,alpha);
    }
}
