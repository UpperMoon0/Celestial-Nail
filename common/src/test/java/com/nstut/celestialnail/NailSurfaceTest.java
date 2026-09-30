package com.nstut.celestialnail;

import com.nstut.celestialnail.client.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NailSurfaceTest {
    @Test void everyMaterialUsesRealAtlasCoordinatesAndBoundedColorThroughItsLifecycle() {
        for(var mesh:List.of(CelestialNailMesh.BODY,CelestialNailMesh.SHARDS,CelestialNailMesh.PORTAL_CORE,
                CelestialNailMesh.PORTAL_RIM,CelestialNailMesh.PORTAL_HALO,CelestialNailMesh.PORTAL_BEAM,
                CelestialNailMesh.SHOCK_SURFACE,CelestialNailMesh.IMPACT_COLUMN)) {
            for(float age:new float[]{-1,0,.5F,1,180,959.9F,960,100000}) {
                NailMeshClipper.emit(mesh,Float.POSITIVE_INFINITY,(face,point,s,t)->{
                    var color=NailSurface.sample(face,point,s,t,.7F,age);
                    assertTrue(color.u()>=0 && color.u()<=1 && color.v()>=0 && color.v()<=1);
                    for(float c:new float[]{color.red(),color.green(),color.blue(),color.alpha()})
                        assertTrue(Float.isFinite(c) && c>=0 && c<=1);
                });
            }
        }
    }

    @Test void pulseCutsInteriorOfLongFacetAndRetainsInterpolatedUvs() {
        var a=new CelestialNailMesh.Point(0,0,0);var b=new CelestialNailMesh.Point(1,0,0);
        var c=new CelestialNailMesh.Point(1,8,0);var d=new CelestialNailMesh.Point(0,8,0);
        var face=new CelestialNailMesh.Face(a,b,c,d,new CelestialNailMesh.Point(0,0,1),3,1);
        int[] count={0};
        NailMeshClipper.emit(List.of(face),3,4,(f,p,u,v)->{
            count[0]++;assertTrue(p.y()>=3 && p.y()<=4);
            assertEquals(p.x(),u,.000001);assertEquals(p.y()/8,v,.000001);
        });
        assertTrue(count[0]>0);assertEquals(0,count[0]%4);
    }

    @Test void fragmentNormalsFollowGeometryInsteadOfStayingInTheOriginalOrientation() {
        var piece=CelestialNailFracture.BODY.get(0);
        var motion=CelestialNailFracture.motion(piece,30);
        var face=piece.faces().get(0);
        var normal=motion.rotateNormal(face.normal());
        var a=motion.apply(face.a());var b=motion.apply(face.b());var c=motion.apply(face.c());
        assertEquals(1,normal.x()*normal.x()+normal.y()*normal.y()+normal.z()*normal.z(),.00001);
        assertEquals(0,normal.x()*(b.x()-a.x())+normal.y()*(b.y()-a.y())+normal.z()*(b.z()-a.z()),.00001);
        assertEquals(0,normal.x()*(c.x()-a.x())+normal.y()*(c.y()-a.y())+normal.z()*(c.z()-a.z()),.00001);
    }

    @Test void shockwaveHasAnOpenCenterAndFiniteUnitNormals() {
        assertTrue(CelestialNailMesh.SHOCK_SURFACE.size()<1000);
        for(var face:CelestialNailMesh.SHOCK_SURFACE) {
            assertEquals(1,Math.abs(face.normal().y()),.00001);
            for(var p:List.of(face.a(),face.b(),face.c(),face.d())) {
                float radius=(float)Math.hypot(p.x(),p.z());
                assertTrue(radius>=.85F && radius<=.95F);
            }
        }
    }
}
