package com.nstut.celestialnail;

import com.nstut.celestialnail.client.CelestialNailMesh;
import com.nstut.celestialnail.client.NailMeshClipper;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class NailMeshClipperTest {
    @Test void emergenceNeverDrawsGeometryAboveThePortalAndPreservesUvs() {
        for(float ceiling:new float[]{-1,0,.1F,1,3,6.8F,7.3F,8}) {
            var count=new AtomicInteger();
            NailMeshClipper.emit(CelestialNailMesh.BODY,ceiling,(face,p,u,v)->{
                assertTrue(p.y()<=ceiling+.00001F);
                assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.z()));
                assertTrue(u>=0 && u<=1 && v>=0 && v<=1);
                count.incrementAndGet();
            });
            assertEquals(0,count.get()%4);
            if(ceiling<0) assertEquals(0,count.get());
            if(ceiling==8) assertEquals(CelestialNailMesh.BODY.size()*4,count.get());
        }
    }
    @Test void portalGeometryHasValidNormals() {
        for(var mesh:java.util.List.of(CelestialNailMesh.PORTAL_CORE,CelestialNailMesh.PORTAL_RIM,
                CelestialNailMesh.PORTAL_HALO,CelestialNailMesh.PORTAL_SPARKS,CelestialNailMesh.PORTAL_BEAM)) {
            assertFalse(mesh.isEmpty());
            for(var face:mesh) {
                var n=face.normal();
                assertEquals(1,n.x()*n.x()+n.y()*n.y()+n.z()*n.z(),.001);
                assertTrue(face.emissive());
            }
        }
    }
}
