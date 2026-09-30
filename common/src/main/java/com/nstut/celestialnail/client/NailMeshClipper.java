package com.nstut.celestialnail.client;

import java.util.ArrayList;
import java.util.List;
import com.nstut.celestialnail.client.CelestialNailMesh.Face;
import com.nstut.celestialnail.client.CelestialNailMesh.Point;

/** Clips the emerging nail at the portal plane, interpolating UVs at the cut edge. */
public final class NailMeshClipper {
    @FunctionalInterface
    public interface Sink { void vertex(Face face, Point point, float u, float v); }
    private record Vertex(Point point, float u, float v) {}
    private NailMeshClipper() {}
    public static void emit(List<Face> faces, float ceiling, Sink sink) {
        emit(faces,Float.NEGATIVE_INFINITY,ceiling,sink);
    }
    public static void emit(List<Face> faces,float floor,float ceiling,Sink sink) {
        for(Face face:faces) {
            float max=Math.max(Math.max(face.a().y(),face.b().y()),Math.max(face.c().y(),face.d().y()));
            float min=Math.min(Math.min(face.a().y(),face.b().y()),Math.min(face.c().y(),face.d().y()));
            if(min>ceiling || max<floor) continue;
            if(max<=ceiling && min>=floor) {
                sink.vertex(face,face.a(),0,0); sink.vertex(face,face.b(),1,0);
                sink.vertex(face,face.c(),1,1); sink.vertex(face,face.d(),0,1);
                continue;
            }
            List<Vertex> input=List.of(new Vertex(face.a(),0,0),new Vertex(face.b(),1,0),new Vertex(face.c(),1,1),new Vertex(face.d(),0,1));
            List<Vertex> clipped=clip(clip(input,ceiling,false),floor,true);
            for(int i=1;i<clipped.size()-1;i++) {
                Vertex a=clipped.get(0),b=clipped.get(i),c=clipped.get(i+1);
                sink.vertex(face,a.point,a.u,a.v); sink.vertex(face,b.point,b.u,b.v);
                sink.vertex(face,c.point,c.u,c.v); sink.vertex(face,c.point,c.u,c.v);
            }
        }
    }
    private static List<Vertex> clip(List<Vertex> input,float height,boolean above) {
        if(input.isEmpty())return input;
        List<Vertex> clipped=new ArrayList<>(6);
        Vertex previous=input.get(input.size()-1);
        for(Vertex current:input) {
            boolean a=above?previous.point.y()>=height:previous.point.y()<=height;
            boolean b=above?current.point.y()>=height:current.point.y()<=height;
            if(a!=b) {
                float t=(height-previous.point.y())/(current.point.y()-previous.point.y());
                Point p=previous.point,q=current.point;
                clipped.add(new Vertex(new Point(p.x()+(q.x()-p.x())*t,height,p.z()+(q.z()-p.z())*t),
                        previous.u+(current.u-previous.u)*t,previous.v+(current.v-previous.v)*t));
            }
            if(b)clipped.add(current);
            previous=current;
        }
        return clipped;
    }

}
