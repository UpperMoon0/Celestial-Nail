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
        for(Face face:faces) {
            float max=Math.max(Math.max(face.a().y(),face.b().y()),Math.max(face.c().y(),face.d().y()));
            float min=Math.min(Math.min(face.a().y(),face.b().y()),Math.min(face.c().y(),face.d().y()));
            if(min>ceiling) continue;
            if(max<=ceiling) {
                sink.vertex(face,face.a(),0,0); sink.vertex(face,face.b(),1,0);
                sink.vertex(face,face.c(),1,1); sink.vertex(face,face.d(),0,1);
                continue;
            }
            Vertex[] input={new Vertex(face.a(),0,0),new Vertex(face.b(),1,0),new Vertex(face.c(),1,1),new Vertex(face.d(),0,1)};
            List<Vertex> clipped=new ArrayList<>(5);
            Vertex previous=input[3];
            for(Vertex current:input) {
                boolean a=previous.point.y()<=ceiling,b=current.point.y()<=ceiling;
                if(a!=b) {
                    float t=(ceiling-previous.point.y())/(current.point.y()-previous.point.y());
                    Point p=previous.point, q=current.point;
                    clipped.add(new Vertex(new Point(p.x()+(q.x()-p.x())*t,ceiling,p.z()+(q.z()-p.z())*t),
                            previous.u+(current.u-previous.u)*t,previous.v+(current.v-previous.v)*t));
                }
                if(b) clipped.add(current);
                previous=current;
            }
            for(int i=1;i<clipped.size()-1;i++) {
                Vertex a=clipped.get(0),b=clipped.get(i),c=clipped.get(i+1);
                sink.vertex(face,a.point,a.u,a.v); sink.vertex(face,b.point,b.u,b.v);
                sink.vertex(face,c.point,c.u,c.v); sink.vertex(face,c.point,c.u,c.v);
            }
        }
    }
}
